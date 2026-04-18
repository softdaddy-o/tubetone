package com.tubetone.extract

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class AudioDownloaderTest {
    private lateinit var server: MockWebServer
    private lateinit var tmpDir: File

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        tmpDir = createTempDir("downloader-test")
    }

    @After fun tearDown() {
        server.shutdown()
        tmpDir.deleteRecursively()
    }

    @Test fun `streams body to destination and emits progress`() = runBlocking {
        val payload = ByteArray(1024 * 50) { (it % 256).toByte() }
        val body = Buffer().write(payload)
        server.enqueue(MockResponse().setHeader("Content-Length", payload.size.toString()).setBody(body))

        val downloader = AudioDownloader(OkHttpClient())
        val dest = File(tmpDir, "out.m4a")
        val progress = downloader.download(server.url("/").toString(), dest).toList()

        assertTrue(dest.exists())
        assertEquals(payload.size.toLong(), dest.length())
        assertTrue("expected progress events", progress.isNotEmpty())
        assertEquals(1.0f, progress.last(), 0.001f)
    }
}
