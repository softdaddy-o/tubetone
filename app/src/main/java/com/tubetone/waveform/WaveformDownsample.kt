package com.tubetone.waveform

import kotlin.math.abs
import kotlin.math.ceil

object WaveformDownsample {
    fun bucketPeaks(samples: FloatArray, targetBuckets: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(0)
        val buckets = FloatArray(targetBuckets)
        val perBucket = ceil(samples.size.toDouble() / targetBuckets).toInt().coerceAtLeast(1)
        for (i in buckets.indices) {
            val start = i * perBucket
            if (start >= samples.size) { buckets[i] = 0f; continue }
            val end = (start + perBucket).coerceAtMost(samples.size)
            var peak = 0f
            for (j in start until end) { val v = abs(samples[j]); if (v > peak) peak = v }
            buckets[i] = peak
        }
        return buckets
    }
}
