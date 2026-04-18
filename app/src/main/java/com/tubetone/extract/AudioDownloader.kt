package com.tubetone.extract

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class AudioDownloader(private val client: OkHttpClient) {
    fun download(url: String, destination: File): Flow<Float> = flow {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val total = resp.header("Content-Length")?.toLongOrNull() ?: -1L
            val source = resp.body?.source() ?: error("no body")
            destination.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                var read = 0L
                var lastEmitAt = 0L
                while (true) {
                    val n = source.read(buffer)
                    if (n <= 0) break
                    out.write(buffer, 0, n)
                    read += n
                    if (total > 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastEmitAt > 100 || read == total) {
                            emit((read.toFloat() / total).coerceIn(0f, 1f))
                            lastEmitAt = now
                        }
                    }
                }
            }
            emit(1.0f)
        }
    }.flowOn(Dispatchers.IO)
}
