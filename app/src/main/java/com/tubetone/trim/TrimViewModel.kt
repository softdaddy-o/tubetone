package com.tubetone.trim

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubetone.extract.VideoMetadata
import com.tubetone.library.db.RingtoneSource
import com.tubetone.waveform.WaveformSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class TrimUiState(
    val metadata: VideoMetadata,
    val audioFile: File,
    val samples: FloatArray,
    val selection: WaveformSelection = WaveformSelection(0f, 0.05f),
    val fadeEnabled: Boolean = true,
    val loopPreview: Boolean = true,
    /** Where the audio came from — drives source pill and library `source` column. */
    val source: RingtoneSource = RingtoneSource.YOUTUBE,
    /** Source file codec MIME type, e.g. "audio/mp4a-latm". Null until probed. */
    val sourceCodec: String? = null,
    /** Source file bitrate in bps. Null until probed or if not reported by extractor. */
    val sourceBitrate: Int? = null,
    /** Output bitrate in kbps (only applies when fade=true re-encode path). */
    val outputBitrateKbps: Int = 128
) {
    val startMs: Long get() = (selection.startFrac * metadata.durationMs).toLong()
    val endMs: Long get() = (selection.endFrac * metadata.durationMs).toLong()
    val segmentMs: Long get() = endMs - startMs
    override fun equals(other: Any?) = other is TrimUiState && metadata == other.metadata && selection == other.selection && fadeEnabled == other.fadeEnabled && source == other.source
    override fun hashCode(): Int = listOf(metadata, selection, fadeEnabled, source).hashCode()
}

class TrimViewModel(initial: TrimUiState) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<TrimUiState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(initial.audioFile.absolutePath)
                val track = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                }
                if (track != null) {
                    val fmt = extractor.getTrackFormat(track)
                    val codec = fmt.getString(MediaFormat.KEY_MIME)
                    val bitrate = runCatching { fmt.getInteger(MediaFormat.KEY_BIT_RATE) }.getOrNull()
                    _state.value = _state.value.copy(sourceCodec = codec, sourceBitrate = bitrate)
                }
            } finally {
                extractor.release()
            }
        }
    }

    fun updateSelection(sel: WaveformSelection) { _state.value = _state.value.copy(selection = sel) }
    fun toggleFade() { _state.value = _state.value.copy(fadeEnabled = !_state.value.fadeEnabled) }
    fun toggleLoop() { _state.value = _state.value.copy(loopPreview = !_state.value.loopPreview) }
    fun setOutputBitrate(kbps: Int) { _state.value = _state.value.copy(outputBitrateKbps = kbps) }
    fun initialThirtySecond() {
        val total = _state.value.metadata.durationMs
        if (total <= 30_000) return
        val startFrac = 0.0f
        val endFrac = (30_000f / total).coerceAtMost(1f)
        _state.value = _state.value.copy(selection = WaveformSelection(startFrac, endFrac))
    }
}
