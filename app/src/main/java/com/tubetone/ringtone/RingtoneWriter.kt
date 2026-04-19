package com.tubetone.ringtone

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class WrittenRingtone(val uri: Uri, val filePath: String)

class RingtoneWriter(private val context: Context) {
    suspend fun writeAsRingtone(
        source: File,
        displayName: String,
        slot: RingtoneSlot = RingtoneSlot.Ringtone
    ): WrittenRingtone = withContext(Dispatchers.IO) {
        val safeName = displayName.replace(Regex("""[\\/:*?"<>|]"""), "_").take(80)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "TubeTone_$safeName.m4a")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            slot.applyMediaStoreFlags(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, slot.relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: error("MediaStore insert returned null")
        resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
            ?: error("Could not open MediaStore output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        val filePath = resolveFilePath(uri)
        WrittenRingtone(uri, filePath)
    }

    private fun resolveFilePath(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: uri.toString()
        }
        return uri.toString()
    }
}
