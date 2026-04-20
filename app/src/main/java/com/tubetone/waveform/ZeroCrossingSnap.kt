package com.tubetone.waveform

/**
 * Snap a sample index to the nearest zero-crossing in a PCM buffer so cuts
 * don't click/pop. None of the top competitors do this — cheap win, big UX
 * signal. v0.2.0 MUST (M2).
 *
 * Called on downsampled envelope buckets (what the UI sees) or raw PCM
 * (what the trimmer sees). Pure function — trivial to unit test.
 */
object ZeroCrossingSnap {

    /**
     * Find a zero-crossing in [amplitudes] within [windowRadius] samples of
     * [targetIndex]. A zero-crossing is where the waveform envelope crosses
     * zero; for our envelope buckets (non-negative peaks) we approximate with
     * the nearest local minimum — a low-amplitude sample is functionally a
     * zero-crossing for click-suppression purposes.
     *
     * @param amplitudes waveform envelope (all values >= 0; typical range 0..1)
     * @param targetIndex where the user dropped the handle
     * @param windowRadius how far to search on each side (±30 samples ≈ 0.7ms at 44.1kHz raw,
     *                     or a few envelope buckets on downsampled data)
     * @return snapped index, or [targetIndex] if [amplitudes] is empty / windowRadius <= 0
     */
    fun snap(
        amplitudes: FloatArray,
        targetIndex: Int,
        windowRadius: Int = 30
    ): Int {
        if (amplitudes.isEmpty() || windowRadius <= 0) return targetIndex
        val clampedTarget = targetIndex.coerceIn(0, amplitudes.size - 1)
        val lo = (clampedTarget - windowRadius).coerceAtLeast(0)
        val hi = (clampedTarget + windowRadius).coerceAtMost(amplitudes.size - 1)
        var best = clampedTarget
        var bestAmp = amplitudes[clampedTarget]
        for (i in lo..hi) {
            val amp = amplitudes[i]
            // Prefer lower amplitude; ties go to the sample nearest the target.
            if (amp < bestAmp ||
                (amp == bestAmp && kotlin.math.abs(i - clampedTarget) < kotlin.math.abs(best - clampedTarget))
            ) {
                best = i
                bestAmp = amp
            }
        }
        return best
    }

    /**
     * Fraction-space convenience for the waveform UI. Maps [targetFrac] into
     * the [amplitudes] array, snaps, then returns the result as a fraction.
     */
    fun snapFrac(
        amplitudes: FloatArray,
        targetFrac: Float,
        windowRadius: Int = 4
    ): Float {
        if (amplitudes.isEmpty()) return targetFrac.coerceIn(0f, 1f)
        val targetIndex = (targetFrac.coerceIn(0f, 1f) * (amplitudes.size - 1)).toInt()
        val snapped = snap(amplitudes, targetIndex, windowRadius)
        return snapped.toFloat() / (amplitudes.size - 1).coerceAtLeast(1)
    }
}
