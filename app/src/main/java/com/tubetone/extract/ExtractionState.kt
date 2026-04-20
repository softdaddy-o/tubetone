package com.tubetone.extract

/** Which pipeline produced the audio now in [ExtractionState.Ready]. */
enum class ExtractionSource { YOUTUBE, LOCAL }

sealed interface ExtractionState {
    object Idle : ExtractionState
    data class FetchingMetadata(val videoId: String) : ExtractionState
    data class Downloading(val metadata: VideoMetadata, val progress: Float) : ExtractionState
    data class AnalyzingWaveform(val metadata: VideoMetadata) : ExtractionState
    data class Ready(
        val metadata: VideoMetadata,
        val audioFile: java.io.File,
        val waveform: FloatArray,
        val source: ExtractionSource = ExtractionSource.YOUTUBE
    ) : ExtractionState {
        override fun equals(other: Any?) = other is Ready && metadata == other.metadata && audioFile == other.audioFile && source == other.source
        override fun hashCode(): Int = metadata.hashCode() * 31 + audioFile.hashCode() + source.hashCode()
    }
    data class Failed(val reason: FailureReason, val cause: Throwable?) : ExtractionState
}

enum class FailureReason { NETWORK, EXTRACTOR_BROKEN, TOO_LONG, CANCELLED, UNKNOWN }
