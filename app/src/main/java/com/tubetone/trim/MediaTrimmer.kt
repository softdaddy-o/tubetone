package com.tubetone.trim

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object MediaTrimmer {

    /**
     * Copy-mode trim: extract a [startMs, endMs] slice of the audio track and mux into a new MP4/M4A.
     * No decode or re-encode. v1 fade toggle currently falls back to copy mode.
     */
    suspend fun trim(params: TrimParams): Unit = withContext(Dispatchers.IO) {
        val output = File(params.outputPath).apply { parentFile?.mkdirs(); if (exists()) delete() }
        val extractor = MediaExtractor().apply { setDataSource(params.inputPath) }
        val audioTrack = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run { extractor.release(); error("no audio track in ${params.inputPath}") }
        val format = extractor.getTrackFormat(audioTrack)
        extractor.selectTrack(audioTrack)

        val muxer = MediaMuxer(params.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val outTrack = muxer.addTrack(format)
        muxer.start()

        val bufferSize = format.getIntegerOr(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024)
        val buf: ByteBuffer = ByteBuffer.allocate(bufferSize)
        val info = MediaCodec.BufferInfo()
        try {
            extractor.seekTo(params.startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val baseTimeUs = extractor.sampleTime.coerceAtLeast(0)
            while (true) {
                buf.clear()
                val size = extractor.readSampleData(buf, 0)
                if (size < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > params.endUs) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = sampleTimeUs - baseTimeUs
                info.flags = extractor.sampleFlagsCompat()
                muxer.writeSampleData(outTrack, buf, info)
                if (!extractor.advance()) break
            }
        } finally {
            runCatching { muxer.stop() }
            muxer.release()
            extractor.release()
        }
        if (!output.exists() || output.length() == 0L) error("trim produced empty output")
    }

    private fun MediaFormat.getIntegerOr(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default

    private fun MediaExtractor.sampleFlagsCompat(): Int {
        val raw = sampleFlags
        var out = 0
        if (raw and MediaExtractor.SAMPLE_FLAG_SYNC != 0) out = out or MediaCodec.BUFFER_FLAG_KEY_FRAME
        return out
    }
}

// TODO(v1.1): implement fade mode via decode -> apply linear gain envelope -> AAC encoder -> muxer.
// Until then the TrimScreen should show a snackbar when fade is toggled on: "페이드 효과는 다음 업데이트에서 지원됩니다".
