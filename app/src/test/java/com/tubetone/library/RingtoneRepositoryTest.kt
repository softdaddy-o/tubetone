package com.tubetone.library

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tubetone.library.db.TubeToneDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RingtoneRepositoryTest {
    private lateinit var db: TubeToneDatabase
    private lateinit var repo: RingtoneRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TubeToneDatabase::class.java).build()
        repo = RingtoneRepository(db.ringtoneDao())
    }
    @After fun tearDown() = db.close()

    @Test fun `detects duplicates by videoId + range`() = runBlocking {
        val r = fixture("a", vid = "vid1", start = 1000, end = 31000)
        repo.save(r)
        assertNotNull(repo.findDuplicate("vid1", 1000, 31000))
        assertNull(repo.findDuplicate("vid1", 1000, 30000))
    }

    @Test fun `mark applied updates timestamp`() = runBlocking {
        val r = fixture("b", vid = "vid2", start = 0, end = 10_000)
        repo.save(r)
        repo.markApplied("b", 123L)
        val updated = repo.findDuplicate("vid2", 0, 10_000)
        assertEquals(123L, updated?.lastAppliedAt)
    }

    private fun fixture(id: String, vid: String, start: Long, end: Long) =
        com.tubetone.library.db.RingtoneEntity(
            id = id, title = id, sourceUrl = "https://youtu.be/$vid", sourceVideoId = vid,
            sourceTitle = "t", thumbnailUrl = null, startMs = start, endMs = end, durationMs = end - start,
            fadeEnabled = false, outputUri = "content://x", outputFilePath = "/x", originalCachePath = null,
            createdAt = 0, lastAppliedAt = null
        )
}
