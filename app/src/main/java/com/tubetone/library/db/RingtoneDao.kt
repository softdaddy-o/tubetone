package com.tubetone.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RingtoneDao {
    @Query("SELECT * FROM ringtones ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RingtoneEntity>>

    @Query("SELECT * FROM ringtones WHERE sourceVideoId = :vid AND startMs = :start AND endMs = :end LIMIT 1")
    suspend fun findDuplicate(vid: String, start: Long, end: Long): RingtoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RingtoneEntity)

    @Query("DELETE FROM ringtones WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE ringtones SET lastAppliedAt = :ts WHERE id = :id")
    suspend fun markApplied(id: String, ts: Long)
}
