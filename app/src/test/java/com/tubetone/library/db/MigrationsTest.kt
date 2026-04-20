package com.tubetone.library.db

import android.content.ContentValues
import android.media.RingtoneManager
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Room migration test for v1 → v2 schema.
 *
 * Runs under Robolectric. Exported schemas are copied into
 * `app/src/test/assets/` so the helper can load them via the test-context
 * assets. The gradle task `copyTestSchemas` keeps the copy in sync.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationsTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TubeToneDatabase::class.java
    )

    @Test
    fun `migrate 1 to 2 adds slotType and source columns with defaults`() {
        val dbName = "migration-test-${System.nanoTime()}.db"

        // Create v1 with one row. v1 schema has no slotType / source.
        helper.createDatabase(dbName, 1).use { db ->
            val v = ContentValues().apply {
                put("id", "abc")
                put("title", "hello")
                put("sourceUrl", "https://youtu.be/abc")
                put("sourceVideoId", "abc")
                put("sourceTitle", "Hello Song")
                putNull("thumbnailUrl")
                put("startMs", 0L)
                put("endMs", 10_000L)
                put("durationMs", 10_000L)
                put("fadeEnabled", 0)
                put("outputUri", "content://mock/1")
                put("outputFilePath", "/tmp/out.m4a")
                putNull("originalCachePath")
                put("createdAt", 1L)
                putNull("lastAppliedAt")
            }
            db.insert("ringtones", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, v)
        }

        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT slotType, source FROM ringtones WHERE id = 'abc'").use { c ->
                assertEquals(true, c.moveToFirst())
                assertEquals(RingtoneManager.TYPE_RINGTONE, c.getInt(0))
                assertEquals("YOUTUBE", c.getString(1))
            }
        }
    }
}
