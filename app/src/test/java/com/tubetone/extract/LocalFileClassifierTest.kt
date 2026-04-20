package com.tubetone.extract

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalFileClassifierTest {

    @Test
    fun `audio MIME returns AUDIO`() {
        assertEquals(LocalFileClassifier.Kind.AUDIO, LocalFileClassifier.classify("audio/mpeg"))
        assertEquals(LocalFileClassifier.Kind.AUDIO, LocalFileClassifier.classify("audio/mp4"))
        assertEquals(LocalFileClassifier.Kind.AUDIO, LocalFileClassifier.classify("audio/x-wav"))
    }

    @Test
    fun `video MIME returns VIDEO`() {
        assertEquals(LocalFileClassifier.Kind.VIDEO, LocalFileClassifier.classify("video/mp4"))
        assertEquals(LocalFileClassifier.Kind.VIDEO, LocalFileClassifier.classify("video/webm"))
    }

    @Test
    fun `non media MIME returns UNSUPPORTED`() {
        assertEquals(LocalFileClassifier.Kind.UNSUPPORTED, LocalFileClassifier.classify("text/plain"))
        assertEquals(LocalFileClassifier.Kind.UNSUPPORTED, LocalFileClassifier.classify("image/png"))
    }

    @Test
    fun `mime case is normalised`() {
        assertEquals(LocalFileClassifier.Kind.AUDIO, LocalFileClassifier.classify("AUDIO/MP4"))
    }

    @Test
    fun `extension fallback for audio when mime is null`() {
        assertEquals(LocalFileClassifier.Kind.AUDIO, LocalFileClassifier.classify(null, "song.m4a"))
        assertEquals(LocalFileClassifier.Kind.AUDIO, LocalFileClassifier.classify(null, "song.mp3"))
        assertEquals(LocalFileClassifier.Kind.AUDIO, LocalFileClassifier.classify(null, "song.FLAC"))
    }

    @Test
    fun `extension fallback for video when mime is null`() {
        assertEquals(LocalFileClassifier.Kind.VIDEO, LocalFileClassifier.classify(null, "clip.mp4"))
        assertEquals(LocalFileClassifier.Kind.VIDEO, LocalFileClassifier.classify(null, "clip.mkv"))
    }

    @Test
    fun `unknown extension falls through to UNSUPPORTED`() {
        assertEquals(LocalFileClassifier.Kind.UNSUPPORTED, LocalFileClassifier.classify(null, "readme.txt"))
        assertEquals(LocalFileClassifier.Kind.UNSUPPORTED, LocalFileClassifier.classify(null, null))
    }
}
