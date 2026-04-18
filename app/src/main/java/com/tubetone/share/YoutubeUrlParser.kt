package com.tubetone.share

object YoutubeUrlParser {
    private val PATTERNS = listOf(
        Regex("""(?:youtube\.com/watch\?[^ ]*\bv=|youtu\.be/|youtube\.com/shorts/|youtube\.com/embed/)([A-Za-z0-9_-]{11})""")
    )

    fun extractVideoId(input: String?): String? {
        if (input.isNullOrBlank()) return null
        for (pattern in PATTERNS) {
            val match = pattern.find(input) ?: continue
            return match.groupValues[1]
        }
        return null
    }

    fun canonicalUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"
}
