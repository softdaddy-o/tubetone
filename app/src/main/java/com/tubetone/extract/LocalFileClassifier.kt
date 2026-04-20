package com.tubetone.extract

/**
 * Classifies a picked local file (content URI) by its MIME type so the host can
 * decide whether to feed it straight to the audio decode path or pull the
 * audio track out of a video container first. Pure function so it unit-tests
 * without a live ContentResolver.
 */
object LocalFileClassifier {

    enum class Kind {
        /** Pure audio file (m4a, mp3, ogg, wav, flac, aac, ...). */
        AUDIO,
        /** Video container whose audio track we'll extract. */
        VIDEO,
        /** Unknown / unsupported MIME — caller should surface an error. */
        UNSUPPORTED
    }

    /**
     * @param mimeType MIME string from ContentResolver.getType(uri), or null.
     * @param displayName optional filename fallback; used only if mimeType is null.
     */
    fun classify(mimeType: String?, displayName: String? = null): Kind {
        val mt = mimeType?.lowercase()
        if (mt != null) {
            if (mt.startsWith("audio/")) return Kind.AUDIO
            if (mt.startsWith("video/")) return Kind.VIDEO
            return Kind.UNSUPPORTED
        }
        // Fallback on extension — some pickers don't carry MIME types.
        val ext = displayName?.substringAfterLast('.', "")?.lowercase() ?: return Kind.UNSUPPORTED
        return when (ext) {
            "m4a", "mp3", "aac", "ogg", "oga", "opus", "wav", "flac" -> Kind.AUDIO
            "mp4", "m4v", "mkv", "webm", "3gp", "mov" -> Kind.VIDEO
            else -> Kind.UNSUPPORTED
        }
    }
}
