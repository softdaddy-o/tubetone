package com.tubetone.trim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * TDD for [PcmFadeProcessor]. Pure ShortArray → ShortArray with a linear
 * fade-in at the head and fade-out at the tail, in samples (mono) or frames
 * (stereo, both channels receive identical gain per frame).
 */
class PcmFadeProcessorTest {

    @Test
    fun `no fade returns identical samples`() {
        val input = ShortArray(1000) { 10_000 }
        val out = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 0,
            fadeOutMs = 0
        )
        assertEquals(input.size, out.size)
        for (i in input.indices) assertEquals("sample $i", input[i], out[i])
    }

    @Test
    fun `fade-in ramps first sample to zero`() {
        // 10ms of 44.1 kHz mono = 441 samples; first sample's gain is ~0.
        val input = ShortArray(2000) { 10_000 }
        val out = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 10,
            fadeOutMs = 0
        )
        assertEquals(0, out[0].toInt())
        // After the fade-in window, samples should be roughly unchanged.
        assertEquals(10_000, out[1000].toInt())
    }

    @Test
    fun `fade-out ramps last sample to near zero`() {
        val input = ShortArray(2000) { 10_000 }
        val out = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 0,
            fadeOutMs = 10
        )
        // Last sample should be at or very close to 0 (linear ramp).
        assertTrue("last sample = ${out.last()}", abs(out.last().toInt()) < 50)
        // Before the fade-out window, unchanged.
        assertEquals(10_000, out[0].toInt())
    }

    @Test
    fun `fade-in and fade-out both applied`() {
        val input = ShortArray(4000) { 10_000 }
        val out = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 10,
            fadeOutMs = 10
        )
        assertEquals(0, out[0].toInt())
        assertTrue(abs(out.last().toInt()) < 50)
        // Mid-section untouched (~2000).
        assertEquals(10_000, out[2000].toInt())
    }

    @Test
    fun `stereo applies identical gain to both channels of same frame`() {
        // Stereo interleaved: L, R, L, R, ...
        val input = ShortArray(2000) { if (it % 2 == 0) 10_000 else 8_000 }
        val out = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 2,
            fadeInMs = 10,
            fadeOutMs = 0
        )
        // Frame 0: both channels = 0 (gain=0).
        assertEquals(0, out[0].toInt())
        assertEquals(0, out[1].toInt())
        // A frame inside the ramp (say frame 100): L and R have the same gain,
        // so their ratio must match the input ratio 10_000 : 8_000.
        val lIn = 10_000
        val rIn = 8_000
        val lOut = out[200].toInt()
        val rOut = out[201].toInt()
        if (lOut != 0) {
            val gainL = lOut.toDouble() / lIn
            val gainR = rOut.toDouble() / rIn
            assertTrue(
                "L gain $gainL should equal R gain $gainR at frame 100",
                abs(gainL - gainR) < 0.01
            )
        }
    }

    @Test
    fun `fade longer than buffer is clamped to buffer length`() {
        // 100 samples at 44.1 kHz is ~2.27 ms; asking for 100 ms fade should
        // clamp to the buffer length (not throw, not over-index).
        val input = ShortArray(100) { 10_000 }
        val out = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 100,
            fadeOutMs = 100
        )
        assertEquals(100, out.size)
        // First sample 0, last sample near 0.
        assertEquals(0, out[0].toInt())
        assertTrue(abs(out.last().toInt()) < 200)
    }

    @Test
    fun `empty input returns empty output`() {
        val out = PcmFadeProcessor.apply(
            ShortArray(0),
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 10,
            fadeOutMs = 10
        )
        assertEquals(0, out.size)
    }

    @Test
    fun `non-uniform input is faded, not replaced`() {
        // Make sure we're multiplying, not overwriting.
        val input = ShortArray(2000) { ((it % 7 - 3) * 1000).toShort() }
        val out = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 0,
            fadeOutMs = 0
        )
        // No fade → identical.
        for (i in input.indices) assertEquals(input[i], out[i])

        // With fade, mid-section matches (no ramp there).
        val faded = PcmFadeProcessor.apply(
            input,
            sampleRate = 44_100,
            channelCount = 1,
            fadeInMs = 5,
            fadeOutMs = 5
        )
        assertEquals(input[1000], faded[1000])
        // But head should differ.
        assertNotEquals(input[0], faded[0])
    }
}
