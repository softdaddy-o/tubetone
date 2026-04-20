package com.tubetone.trim

/**
 * Applies a linear fade-in at the head and fade-out at the tail of a
 * 16-bit signed PCM buffer. Stereo is handled by applying the same gain
 * across both channels of each frame (interleaved L,R,L,R...).
 *
 * Used by the M1 re-encode pipeline when the user enables the "페이드" chip.
 * The copy-trim (MediaExtractor + MediaMuxer) path cannot do this because
 * it doesn't decode the audio; the re-encode path decodes to PCM, feeds
 * frames through [apply], and re-encodes to AAC.
 *
 * Pure — no Android dependencies. Fully unit-testable.
 */
object PcmFadeProcessor {
    /**
     * @param input Interleaved 16-bit PCM samples.
     * @param sampleRate Hz (e.g. 44_100, 48_000).
     * @param channelCount 1 or 2 (mono/stereo).
     * @param fadeInMs Ramp-up duration at the start. Clamped to buffer length.
     * @param fadeOutMs Ramp-down duration at the end. Clamped to buffer length.
     * @return A new ShortArray; input is not mutated.
     */
    fun apply(
        input: ShortArray,
        sampleRate: Int,
        channelCount: Int,
        fadeInMs: Int,
        fadeOutMs: Int
    ): ShortArray {
        if (input.isEmpty()) return ShortArray(0)
        val out = input.copyOf()
        val totalFrames = input.size / channelCount.coerceAtLeast(1)
        if (totalFrames == 0) return out

        val fadeInFrames = ((fadeInMs.toLong() * sampleRate) / 1000L).toInt()
            .coerceAtLeast(0)
            .coerceAtMost(totalFrames)
        val fadeOutFrames = ((fadeOutMs.toLong() * sampleRate) / 1000L).toInt()
            .coerceAtLeast(0)
            .coerceAtMost(totalFrames)

        // Fade-in: gain ramps 0 → 1 linearly over fadeInFrames.
        if (fadeInFrames > 0) {
            for (f in 0 until fadeInFrames) {
                val gain = f.toFloat() / fadeInFrames.toFloat()
                val base = f * channelCount
                for (c in 0 until channelCount) {
                    val idx = base + c
                    if (idx < out.size) out[idx] = (out[idx] * gain).toInt().toShort()
                }
            }
        }

        // Fade-out: gain ramps 1 → 0 linearly over last fadeOutFrames.
        if (fadeOutFrames > 0) {
            val start = totalFrames - fadeOutFrames
            for (f in 0 until fadeOutFrames) {
                // gain = 1 at frame 0 of the ramp, 0 at the last frame.
                val gain = 1f - (f.toFloat() / fadeOutFrames.toFloat())
                val base = (start + f) * channelCount
                for (c in 0 until channelCount) {
                    val idx = base + c
                    if (idx < out.size) out[idx] = (out[idx] * gain).toInt().toShort()
                }
            }
        }

        return out
    }
}
