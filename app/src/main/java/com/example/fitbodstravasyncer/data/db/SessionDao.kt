package com.example.fitbodstravasyncer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllOnce(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: SessionEntity)

    @Query("UPDATE sessions SET stravaId = :stravaId WHERE id = :sessionId")
    suspend fun updateStravaId(sessionId: String, stravaId: Long?)

    @Query("DELETE FROM sessions WHERE startTime < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM sessions WHERE id IN(:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM sessions WHERE ABS((startTime / 1000) - :startEpoch) < :toleranceSeconds LIMIT 1")
    suspend fun findByStartTimeWithTolerance(startEpoch: Long, toleranceSeconds: Long): SessionEntity?

}
