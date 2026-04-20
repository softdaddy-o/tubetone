package com.tubetone.extract

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Prepares a local content URI for the trim pipeline.
 *
 * Because `MediaExtractor`/`WaveformGenerator` prefer a real file path and
 * scoped-storage content URIs can vanish mid-session, we copy the selected
 * file into the app cache and hand downstream code a concrete File. Audio
 * files land as `.m4a` (container-agnostic — MediaExtractor sniffs by bytes,
 * not extension); video files pass through to the audio track extractor.
 *
 * MIME classification happens in [LocalFileClassifier] — pure and unit-tested.
 */
class LocalFileIngestor(private val context: Context) {

    data class Ingested(
        val file: File,
        val metadata: VideoMetadata,
        val kind: LocalFileClassifier.Kind
    )

    /** Returned if the picker URI is not an audio/video file. */
    class UnsupportedTypeException(val mime: String?) :
        IllegalArgumentException("Unsupported MIME: $mime")

    /**
     * Copy the content URI into the cache and produce a stand-in [VideoMetadata]
     * (so the rest of the pipeline, which is YouTube-shaped, works unchanged).
     */
    suspend fun ingest(uri: Uri): Ingested = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        val displayName = queryDisplayName(uri) ?: "local-${System.currentTimeMillis()}"
        val kind = LocalFileClassifier.classify(mime, displayName)
        if (kind == LocalFileClassifier.Kind.UNSUPPORTED) {
            throw UnsupportedTypeException(mime)
        }

        val cacheDir = File(context.cacheDir, "extracts").apply { mkdirs() }
        val suffix = when (kind) {
            LocalFileClassifier.Kind.AUDIO -> ".m4a"
            LocalFileClassifier.Kind.VIDEO -> ".mp4"
            else -> ".bin"
        }
        val dest = File(cacheDir, "local-${System.currentTimeMillis()}$suffix")
        resolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open stream for $uri")

        val durationMs = probeDurationMs(dest)
        val titleGuess = displayName.substringBeforeLast('.').ifBlank { "Local file" }

        // Stable synthetic ID so duplicate detection works per local file slot + range.
        val syntheticId = "local:${dest.nameWithoutExtension}"
        val metadata = VideoMetadata(
            videoId = syntheticId,
            title = titleGuess,
            thumbnailUrl = null,
            durationMs = durationMs,
            audioStreamUrl = dest.toURI().toString(),
            audioMimeType = mime ?: "audio/mp4"
        )
        Ingested(dest, metadata, kind)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun probeDurationMs(file: File): Long {
        return runCatching {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(file.absolutePath)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        }.getOrElse { 0L }
    }
}
