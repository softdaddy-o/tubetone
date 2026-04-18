package com.tubetone.waveform

import org.junit.Assert.*
import org.junit.Test

class WaveformDownsampleTest {
    @Test fun `downsamples to target bucket count with peak values`() {
        val samples = FloatArray(1000) { if (it % 10 == 0) 1.0f else 0.1f }
        val result = WaveformDownsample.bucketPeaks(samples, targetBuckets = 100)
        assertEquals(100, result.size)
        assertTrue("expected every bucket to capture a peak", result.all { it >= 0.9f })
    }
    @Test fun `handles fewer samples than buckets by padding zeros`() {
        val samples = floatArrayOf(0.5f, 0.8f, 0.3f)
        val result = WaveformDownsample.bucketPeaks(samples, targetBuckets = 10)
        assertEquals(10, result.size)
        assertEquals(0.8f, result.max(), 0.001f)
    }
    @Test fun `empty samples returns empty`() {
        assertArrayEquals(floatArrayOf(), WaveformDownsample.bucketPeaks(floatArrayOf(), 50), 0f)
    }
}
