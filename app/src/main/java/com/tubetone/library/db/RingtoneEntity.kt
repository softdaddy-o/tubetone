package com.tubetone.library.db

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val lastAppliedAt: Long?
)
