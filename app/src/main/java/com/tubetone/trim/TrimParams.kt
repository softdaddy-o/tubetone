package com.tubetone.trim

data class TrimParams(
    val inputPath: String,
    val outputPath: String,
    val startMs: Long,
    val endMs: Long,
    val fade: Boolean,
    val outputBitrateKbps: Int = 128
) {
    init {
        require(endMs > startMs) { "endMs must be > startMs" }
        require(startMs >= 0) { "startMs must be non-negative" }
    }
    val startUs: Long get() = startMs * 1000
    val endUs: Long get() = endMs * 1000
    val durationMs: Long get() = endMs - startMs
}
