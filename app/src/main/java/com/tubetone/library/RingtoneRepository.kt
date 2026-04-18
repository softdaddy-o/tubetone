package com.tubetone.library

import com.tubetone.library.db.RingtoneDao
import com.tubetone.library.db.RingtoneEntity
import kotlinx.coroutines.flow.Flow

class RingtoneRepository(private val dao: RingtoneDao) {
    val all: Flow<List<RingtoneEntity>> = dao.observeAll()
    suspend fun save(r: RingtoneEntity) = dao.upsert(r)
    suspend fun findDuplicate(vid: String, start: Long, end: Long) = dao.findDuplicate(vid, start, end)
    suspend fun delete(id: String) = dao.delete(id)
    suspend fun markApplied(id: String, ts: Long = System.currentTimeMillis()) = dao.markApplied(id, ts)
}
