package com.tubetone.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeUrlParserTest {
    @Test fun `extracts id from full watch url`() {
        assertEquals("dQw4w9WgXcQ", YoutubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }
    @Test fun `extracts id from short url`() {
        assertEquals("dQw4w9WgXcQ", YoutubeUrlParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
    }
    @Test fun `extracts id from mobile url`() {
        assertEquals("dQw4w9WgXcQ", YoutubeUrlParser.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ&feature=shared"))
    }
    @Test fun `extracts id from shorts url`() {
        assertEquals("abc123XYZ_-", YoutubeUrlParser.extractVideoId("https://www.youtube.com/shorts/abc123XYZ_-"))
    }
    @Test fun `returns null for non-youtube url`() {
        assertNull(YoutubeUrlParser.extractVideoId("https://vimeo.com/12345"))
    }
    @Test fun `returns null for garbage`() {
        assertNull(YoutubeUrlParser.extractVideoId("not a url"))
    }
    @Test fun `finds embedded url inside share text`() {
        val text = "Check this out https://youtu.be/dQw4w9WgXcQ amazing"
        assertEquals("dQw4w9WgXcQ", YoutubeUrlParser.extractVideoId(text))
    }
}
