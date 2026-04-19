package com.tubetone.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeEntryScreenTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val clipboard: ClipboardManager
        get() = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Test fun `truncateMiddle keeps short strings`() {
        assertEquals("abc", truncateMiddle("abc", 40))
    }

    @Test fun `truncateMiddle inserts ellipsis in the middle`() {
        val s = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val out = truncateMiddle(s, 20)
        assertEquals(20, out.length)
        assertEquals(true, out.contains("..."))
    }

    @Test fun `readYoutubeUrlFromClipboard returns null on empty clipboard`() {
        clipboard.clearPrimaryClip()
        assertNull(readYoutubeUrlFromClipboard(ctx))
    }

    @Test fun `readYoutubeUrlFromClipboard returns null when clip has non-youtube text`() {
        clipboard.setPrimaryClip(ClipData.newPlainText("t", "hello world"))
        assertNull(readYoutubeUrlFromClipboard(ctx))
    }

    @Test fun `readYoutubeUrlFromClipboard returns the url when clip has youtube link`() {
        val url = "https://youtu.be/dQw4w9WgXcQ"
        clipboard.setPrimaryClip(ClipData.newPlainText("t", url))
        assertEquals(url, readYoutubeUrlFromClipboard(ctx))
    }

    @Test fun `readYoutubeUrlFromClipboard picks up url embedded in longer text`() {
        val embedded = "check this https://www.youtube.com/watch?v=dQw4w9WgXcQ nice"
        clipboard.setPrimaryClip(ClipData.newPlainText("t", embedded))
        assertEquals(embedded, readYoutubeUrlFromClipboard(ctx))
    }
}
