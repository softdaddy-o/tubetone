package com.tubetone.library.db

import android.media.RingtoneManager
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room schema v1 → v2.
 *
 * Adds [RingtoneEntity.slotType] and [RingtoneEntity.source] columns. v1 rows
 * are backfilled with [RingtoneManager.TYPE_RINGTONE] and "YOUTUBE" — v0.1.x
 * only supported YouTube sources and wrote everything to the ringtone slot.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE ringtones ADD COLUMN slotType INTEGER NOT NULL DEFAULT ${RingtoneManager.TYPE_RINGTONE}"
        )
        db.execSQL(
            "ALTER TABLE ringtones ADD COLUMN source TEXT NOT NULL DEFAULT 'YOUTUBE'"
        )
    }
}
