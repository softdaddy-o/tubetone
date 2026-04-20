package com.tubetone.trim

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * M1 — decode-fade-encode pipeline. Used by [TrimCoordinator] when fade is
 * enabled. Copy-trim (MediaExtractor → MediaMuxer) cannot apply a gain
 * envelope because it never sees PCM; this class decodes the AAC track to
 * 16-bit PCM, runs each decoded buffer through [PcmFadeProcessor], and
 * re-encodes to AAC-LC at 128 kbps.
 *
 * Device-only — MediaCodec is not implemented by Robolectric. Unit-testing
 * covers only [PcmFadeProcessor] (the pure transform). This coordinator is
 * verified by manual on-device smoke.
 *
 * Default fade durations: 100 ms in / 300 ms out — subtle enough to avoid
 * clicks without audibly shortening the usable segment.
 */
object MediaReencoder {

    private const val TIMEOUT_US = 10_000L
    private const val FADE_IN_MS = 100
    private const val FADE_OUT_MS = 300

    suspend fun trimAndFade(params: TrimParams): Unit = withContext(Dispatchers.IO) {
        require(params.fade) { "MediaReencoder is only for fade=true; use MediaTrimmer for copy-trim" }

        val output = File(params.outputPath).apply { parentFile?.mkdirs(); if (exists()) delete() }
        val extractor = MediaExtractor().apply { setDataSource(params.inputPath) }
        val audioTrack = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run { extractor.release(); error("no audio track in ${params.inputPath}") }

        val inputFormat = extractor.getTrackFormat(audioTrack)
        extractor.selectTrack(audioTrack)

        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

        val decoder = MediaCodec.createDecoderByType(inputMime).apply {
            configure(inputFormat, null, null, 0)
            start()
        }

        val outputFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, params.outputBitrateKbps * 1000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }

        val muxer = MediaMuxer(params.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerTrack = -1
        var muxerStarted = false

        val decInfo = MediaCodec.BufferInfo()
        val encInfo = MediaCodec.BufferInfo()

        val totalSegmentMs = params.durationMs
        val totalSegmentUs = totalSegmentMs * 1000L

        extractor.seekTo(params.startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        var sawInputEOS = false
        var sawDecoderEOS = false
        var sawEncoderEOS = false

        try {
            while (!sawEncoderEOS) {
                // 1) Feed extractor → decoder
                if (!sawInputEOS) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        val sampleTimeUs = extractor.sampleTime
                        if (sampleSize < 0 || sampleTimeUs > params.endUs) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, sampleTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                // 2) Drain decoder → PCM → fade → encoder
                if (!sawDecoderEOS) {
                    val outIdx = decoder.dequeueOutputBuffer(decInfo, TIMEOUT_US)
                    when {
                        outIdx >= 0 -> {
                            val decBuf = decoder.getOutputBuffer(outIdx)
                            if (decBuf != null && decInfo.size > 0) {
                                // Trim timestamps relative to startUs.
                                val presentationUs = (decInfo.presentationTimeUs - params.startUs).coerceAtLeast(0)
                                val pcmBytes = ByteArray(decInfo.size)
                                decBuf.position(decInfo.offset)
                                decBuf.limit(decInfo.offset + decInfo.size)
                                decBuf.get(pcmBytes)
                                val pcmShorts = bytesToShortsLe(pcmBytes)
                                // Compute local fade gain based on position inside the segment.
                                val fadedShorts = applyPositionalFade(
                                    pcmShorts,
                                    sampleRate = sampleRate,
                                    channelCount = channelCount,
                                    presentationUs = presentationUs,
                                    totalSegmentUs = totalSegmentUs
                                )
                                val fadedBytes = shortsToBytesLe(fadedShorts)
                                feedEncoder(encoder, fadedBytes, presentationUs, eof = false)
                            }
                            decoder.releaseOutputBuffer(outIdx, false)
                            if (decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                sawDecoderEOS = true
                                feedEncoder(encoder, ByteArray(0), 0, eof = true)
                            }
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    }
                }

                // 3) Drain encoder → muxer
                val encIdx = encoder.dequeueOutputBuffer(encInfo, TIMEOUT_US)
                when {
                    encIdx >= 0 -> {
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            encInfo.size = 0
                        }
                        if (encInfo.size > 0 && muxerStarted) {
                            val buf = encoder.getOutputBuffer(encIdx)!!
                            buf.position(encInfo.offset)
                            buf.limit(encInfo.offset + encInfo.size)
                            muxer.writeSampleData(muxerTrack, buf, encInfo)
                        }
                        encoder.releaseOutputBuffer(encIdx, false)
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawEncoderEOS = true
                        }
                    }
                    encIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxerTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    encIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                }
            }
        } finally {
            runCatching { decoder.stop() }
            runCatching { decoder.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
            extractor.release()
        }

        if (!output.exists() || output.length() == 0L) error("re-encode produced empty output")
    }

    private fun feedEncoder(
        encoder: MediaCodec,
        pcmBytes: ByteArray,
        presentationUs: Long,
        eof: Boolean
    ) {
        var offset = 0
        while (offset < pcmBytes.size || eof) {
            val inIdx = encoder.dequeueInputBuffer(TIMEOUT_US)
            if (inIdx < 0) return
            val buf = encoder.getInputBuffer(inIdx)!!
            buf.clear()
            val chunk = minOf(pcmBytes.size - offset, buf.capacity())
            if (chunk > 0) buf.put(pcmBytes, offset, chunk)
            val flags = if (eof) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            encoder.queueInputBuffer(inIdx, 0, chunk, presentationUs, flags)
            offset += chunk
            if (eof) return
        }
    }

    /**
     * Apply fade based on where the buffer sits in the whole segment timeline.
     * Fade-in runs in [0, FADE_IN_MS], fade-out in [total-FADE_OUT_MS, total].
     * Gets the gain at frame granularity by shifting into the buffer.
     */
    private fun applyPositionalFade(
        pcm: ShortArray,
        sampleRate: Int,
        channelCount: Int,
        presentationUs: Long,
        totalSegmentUs: Long
    ): ShortArray {
        if (pcm.isEmpty()) return pcm
        val fadeInUs = FADE_IN_MS * 1000L
        val fadeOutStartUs = totalSegmentUs - FADE_OUT_MS * 1000L
        val out = pcm.copyOf()
        val frames = pcm.size / channelCount.coerceAtLeast(1)
        for (f in 0 until frames) {
            val frameUs = presentationUs + (f.toLong() * 1_000_000L / sampleRate)
            val gain = when {
                frameUs < fadeInUs -> (frameUs.toFloat() / fadeInUs.toFloat()).coerceIn(0f, 1f)
                frameUs > fadeOutStartUs -> {
                    val remain = (totalSegmentUs - frameUs).toFloat()
                    (remain / (FADE_OUT_MS * 1000f)).coerceIn(0f, 1f)
                }
                else -> 1f
            }
            if (gain < 1f) {
                val base = f * channelCount
                for (c in 0 until channelCount) {
                    val idx = base + c
                    if (idx < out.size) out[idx] = (out[idx] * gain).toInt().toShort()
                }
            }
        }
        return out
    }

    private fun bytesToShortsLe(bytes: ByteArray): ShortArray {
        val out = ShortArray(bytes.size / 2)
        var bi = 0
        var si = 0
        while (si < out.size) {
            val lo = bytes[bi].toInt() and 0xff
            val hi = bytes[bi + 1].toInt()
            out[si] = ((hi shl 8) or lo).toShort()
            bi += 2
            si += 1
        }
        return out
    }

    private fun shortsToBytesLe(shorts: ShortArray): ByteArray {
        val out = ByteArray(shorts.size * 2)
        var bi = 0
        var si = 0
        while (si < shorts.size) {
            val v = shorts[si].toInt()
            out[bi] = (v and 0xff).toByte()
            out[bi + 1] = ((v shr 8) and 0xff).toByte()
            bi += 2
            si += 1
        }
        return out
    }
}

/**
 * Dispatches a [TrimParams] request to the correct pipeline:
 *  * fade=false → [MediaTrimmer.trim] (fast, lossless copy)
 *  * fade=true  → [MediaReencoder.trimAndFade] (decode/encode; slower)
 */
object TrimCoordinator {
    suspend fun run(params: TrimParams) {
        if (params.fade) MediaReencoder.trimAndFade(params)
        else MediaTrimmer.trim(params)
    }
}
