package com.tubetone.waveform

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder

class WaveformGenerator {

    /**
     * Decodes audio via MediaCodec to mono-ish PCM_S16 and returns [targetBuckets] peak amplitudes in [0..1].
     * Uses streaming peak accumulation so memory stays O(targetBuckets), not O(sample count).
     */
    suspend fun generate(audioFile: File, targetBuckets: Int = 512): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
        val audioTrack = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run { extractor.release(); error("no audio track") }
        extractor.selectTrack(audioTrack)
        val inputFormat = extractor.getTrackFormat(audioTrack)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: error("mime missing")
        val durationUs = inputFormat.getLong(MediaFormat.KEY_DURATION)
        val channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime).apply {
            configure(inputFormat, null, null, 0)
            start()
        }

        val bucketPeaks = FloatArray(targetBuckets)
        var sampleIndex = 0L
        val estimatedTotalSamples = estimateSampleCount(inputFormat, durationUs).coerceAtLeast(targetBuckets.toLong())
        val samplesPerBucket = (estimatedTotalSamples / targetBuckets).coerceAtLeast(1L)

        val info = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        val timeoutUs = 10_000L

        try {
            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val idx = codec.dequeueInputBuffer(timeoutUs)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx) ?: continue
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(idx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                if (outIdx >= 0) {
                    if (info.size > 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)!!
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val shorts = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shorts.hasRemaining()) {
                            var frameMax = 0
                            repeat(channels) {
                                if (shorts.hasRemaining()) {
                                    val v = kotlin.math.abs(shorts.get().toInt())
                                    if (v > frameMax) frameMax = v
                                }
                            }
                            val bucket = (sampleIndex / samplesPerBucket).toInt().coerceIn(0, targetBuckets - 1)
                            val norm = frameMax / 32768f
                            if (norm > bucketPeaks[bucket]) bucketPeaks[bucket] = norm
                            sampleIndex++
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEos = true
                }
            }
        } finally {
            codec.stop(); codec.release(); extractor.release()
        }
        bucketPeaks
    }

    private fun estimateSampleCount(format: MediaFormat, durationUs: Long): Long {
        val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44_100
        return (durationUs / 1_000_000.0 * sampleRate).toLong()
    }
}
