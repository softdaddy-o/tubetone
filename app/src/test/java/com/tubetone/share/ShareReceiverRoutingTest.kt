package com.tubetone.share

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import com.tubetone.extract.ExtractionForegroundService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShareReceiverRoutingTest {

    @Test
    fun `youtube url intent schedules video extraction service`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(ctx, ShareReceiverActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://youtu.be/dQw4w9WgXcQ")
        }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { /* finishes immediately */ }

        val shadowApp = shadowOf(ctx as android.app.Application)
        val nextIntent = shadowApp.nextStartedService
        assertNotNull("expected a service start", nextIntent)
        assertEquals(
            ExtractionForegroundService::class.java.name,
            nextIntent.component?.className
        )
        assertEquals("dQw4w9WgXcQ", nextIntent.getStringExtra(ExtractionForegroundService.EXTRA_VIDEO_ID))
    }

    @Test
    fun `audio share intent routes to local ingestion`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val audioUri = Uri.parse("content://com.example/audio/123.m4a")
        val intent = Intent(ctx, ShareReceiverActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, audioUri)
        }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { }

        val shadowApp = shadowOf(ctx as android.app.Application)
        val nextIntent = shadowApp.nextStartedService
        assertNotNull(nextIntent)
        assertEquals(
            audioUri.toString(),
            nextIntent.getStringExtra(ExtractionForegroundService.EXTRA_LOCAL_URI)
        )
    }

    @Test
    fun `video share intent routes to local ingestion`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val videoUri = Uri.parse("content://com.example/video/1.mp4")
        val intent = Intent(ctx, ShareReceiverActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, videoUri)
        }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use { }

        val shadowApp = shadowOf(ctx as android.app.Application)
        val nextIntent = shadowApp.nextStartedService
        assertNotNull(nextIntent)
        assertEquals(
            videoUri.toString(),
            nextIntent.getStringExtra(ExtractionForegroundService.EXTRA_LOCAL_URI)
        )
    }
}
