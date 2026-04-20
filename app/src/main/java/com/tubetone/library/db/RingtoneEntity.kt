package com.tubetone.library.db

import android.media.RingtoneManager
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted ringtone metadata (Room schema v2).
 *
 * v2 migration adds:
 *  - [slotType] — the [RingtoneManager] TYPE_* the file was saved into.
 *    v1 rows are migrated as TYPE_RINGTONE (0x01), matching v0.1.x behaviour.
 *  - [source] — where the audio came from. v1 rows migrate as "YOUTUBE"
 *    (v0.1.x was YouTube-only). See [RingtoneSource].
 */
@Entity(tableName = "ringtones")
data class RingtoneEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val sourceVideoId: String,
    val sourceTitle: String,
    val thumbnailUrl: String?,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val fadeEnabled: Boolean,
    val outputUri: String,
    val outputFilePath: String,
    val originalCachePath: String?,
    val createdAt: Long,
    val lastAppliedAt: Long?,
    // v2 additions:
    val slotType: Int = RingtoneManager.TYPE_RINGTONE,
    val source: String = RingtoneSource.YOUTUBE.name
)
