package com.tubetone.trim

import androidx.lifecycle.ViewModel
import com.tubetone.extract.VideoMetadata
import com.tubetone.waveform.WaveformSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class TrimUiState(
    val metadata: VideoMetadata,
    val audioFile: File,
    val samples: FloatArray,
    val selection: WaveformSelection = WaveformSelection(0f, 0.05f),
    val fadeEnabled: Boolean = true,
    val loopPreview: Boolean = true
) {
    val startMs: Long get() = (selection.startFrac * metadata.durationMs).toLong()
    val endMs: Long get() = (selection.endFrac * metadata.durationMs).toLong()
    val segmentMs: Long get() = endMs - startMs
    override fun equals(other: Any?) = other is TrimUiState && metadata == other.metadata && selection == other.selection && fadeEnabled == other.fadeEnabled
    override fun hashCode(): Int = listOf(metadata, selection, fadeEnabled).hashCode()
}

class TrimViewModel(initial: TrimUiState) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<TrimUiState> = _state
    fun updateSelection(sel: WaveformSelection) { _state.value = _state.value.copy(selection = sel) }
    fun toggleFade() { _state.value = _state.value.copy(fadeEnabled = !_state.value.fadeEnabled) }
    fun toggleLoop() { _state.value = _state.value.copy(loopPreview = !_state.value.loopPreview) }
    fun initialThirtySecond() {
        val total = _state.value.metadata.durationMs
        if (total <= 30_000) return
        val startFrac = 0.0f
        val endFrac = (30_000f / total).coerceAtMost(1f)
        _state.value = _state.value.copy(selection = WaveformSelection(startFrac, endFrac))
    }
}
