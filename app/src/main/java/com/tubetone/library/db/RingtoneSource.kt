package com.tubetone.library.db

/**
 * Origin of a saved ringtone. Persisted as the [RingtoneEntity.source] string
 * column (Room schema v2). `YOUTUBE` is the v0.1.x default; `LOCAL` lands
 * with v0.2.0 local file picker (L1). Future sources get appended here.
 */
enum class RingtoneSource {
    YOUTUBE, LOCAL;

    companion object {
        fun parse(raw: String?): RingtoneSource = when (raw) {
            LOCAL.name -> LOCAL
            else -> YOUTUBE
        }
    }
}
