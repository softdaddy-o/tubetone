package com.tubetone.waveform

import org.junit.Assert.assertEquals
import org.junit.Test

class ZeroCrossingSnapTest {

    @Test
    fun `empty buffer returns target unchanged`() {
        assertEquals(5, ZeroCrossingSnap.snap(FloatArray(0), 5, 30))
    }

    @Test
    fun `zero window returns target unchanged`() {
        val buf = floatArrayOf(0.9f, 0.1f, 0.9f)
        assertEquals(2, ZeroCrossingSnap.snap(buf, 2, 0))
    }

    @Test
    fun `snap picks nearest local minimum within window`() {
        // target 5, nearby minimum at index 3 (amplitude 0.02).
        val buf = floatArrayOf(
            0.9f, 0.8f, 0.7f, 0.02f, 0.6f, // 0..4
            0.7f, 0.5f, 0.9f                // 5..7
        )
        assertEquals(3, ZeroCrossingSnap.snap(buf, 5, 3))
    }

    @Test
    fun `ties go to sample nearest target`() {
        val buf = floatArrayOf(0f, 0.5f, 0.5f, 0f, 0.5f, 0.5f, 0f)
        // target 3 is itself at 0, which is the global min — should return 3.
        assertEquals(3, ZeroCrossingSnap.snap(buf, 3, 3))
    }

    @Test
    fun `window radius clamps at array boundaries`() {
        val buf = floatArrayOf(0.5f, 0.4f, 0.3f, 0.2f)
        assertEquals(3, ZeroCrossingSnap.snap(buf, 3, 100))
    }

    @Test
    fun `target beyond array is clamped`() {
        val buf = floatArrayOf(0.1f, 0.2f, 0.3f)
        assertEquals(0, ZeroCrossingSnap.snap(buf, 99, 2))
    }

    @Test
    fun `snapFrac empty returns original`() {
        assertEquals(0.5f, ZeroCrossingSnap.snapFrac(FloatArray(0), 0.5f), 1e-4f)
    }

    @Test
    fun `snapFrac maps fraction into buffer and back`() {
        // 10 samples, zero crossing at index 4 (amplitude 0).
        val buf = floatArrayOf(0.9f, 0.8f, 0.7f, 0.6f, 0f, 0.6f, 0.7f, 0.8f, 0.9f, 1f)
        val targetFrac = 0.55f // index ~5
        val snapped = ZeroCrossingSnap.snapFrac(buf, targetFrac, 3)
        val expectedFrac = 4f / 9f
        assertEquals(expectedFrac, snapped, 1e-4f)
    }
}
