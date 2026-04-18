package com.tubetone.extract

import com.tubetone.share.YoutubeUrlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import org.schabi.newpipe.extractor.stream.StreamInfo

data class VideoMetadata(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val audioStreamUrl: String,
    val audioMimeType: String
)

class NewPipeExtractorService(
    private val okHttp: OkHttpClient = OkHttpClient()
) {
    init { NewPipe.init(OkHttpDownloader(okHttp)) }

    suspend fun fetch(videoId: String): VideoMetadata = withContext(Dispatchers.IO) {
        val url = YoutubeUrlParser.canonicalUrl(videoId)
        val info = StreamInfo.getInfo(NewPipe.getService(0), url) // 0 = YouTube
        val audio = info.audioStreams
            .filter { it.format?.mimeType?.startsWith("audio/mp4") == true }
            .maxByOrNull { it.averageBitrate }
            ?: info.audioStreams.maxByOrNull { it.averageBitrate }
            ?: error("No audio streams available")
        VideoMetadata(
            videoId = videoId,
            title = info.name,
            thumbnailUrl = info.thumbnails.firstOrNull()?.url,
            durationMs = info.duration * 1000L,
            audioStreamUrl = audio.content,
            audioMimeType = audio.format?.mimeType ?: "audio/mp4"
        )
    }

    private class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {
        override fun execute(request: NpRequest): NpResponse {
            val builder = Request.Builder().url(request.url())
            request.headers().forEach { (k, v) -> v.forEach { builder.addHeader(k, it) } }
            val body = request.dataToSend()?.let { okhttp3.RequestBody.create(null, it) }
            builder.method(request.httpMethod(), body)
            client.newCall(builder.build()).execute().use { resp ->
                return NpResponse(
                    resp.code,
                    resp.message,
                    resp.headers.toMultimap(),
                    resp.body?.string(),
                    resp.request.url.toString()
                )
            }
        }
    }
}
