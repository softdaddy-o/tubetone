package com.tubetone.extract

sealed interface ExtractionState {
    object Idle : ExtractionState
    data class FetchingMetadata(val videoId: String) : ExtractionState
    data class Downloading(val metadata: VideoMetadata, val progress: Float) : ExtractionState
    data class AnalyzingWaveform(val metadata: VideoMetadata) : ExtractionState
    data class Ready(val metadata: VideoMetadata, val audioFile: java.io.File, val waveformFile: java.io.File) : ExtractionState
    data class Failed(val reason: FailureReason, val cause: Throwable?) : ExtractionState
}

enum class FailureReason { NETWORK, EXTRACTOR_BROKEN, TOO_LONG, CANCELLED, UNKNOWN }
