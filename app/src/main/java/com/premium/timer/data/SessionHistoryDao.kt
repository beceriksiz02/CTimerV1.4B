package com.premium.timer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionHistoryDao {
    @Insert
    suspend fun insert(session: SessionHistoryEntity)

    @Query("SELECT * FROM session_history ORDER BY startWallClock DESC")
    fun observeAll(): Flow<List<SessionHistoryEntity>>

    @Query("SELECT * FROM session_history ORDER BY startWallClock DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SessionHistoryEntity>

    @Query("DELETE FROM session_history")
    suspend fun clearAll()

    @Query("SELECT COALESCE(SUM(actualActiveMillis), 0) FROM session_history WHERE startWallClock >= :sinceWallClock")
    suspend fun totalActiveMillisSince(sinceWallClock: Long): Long

    @Query("SELECT COUNT(*) FROM session_history WHERE completed = 1")
    suspend fun completedCount(): Int

    @Query("SELECT COUNT(*) FROM session_history WHERE completed = 0")
    suspend fun abandonedCount(): Int

    @Query("SELECT COALESCE(AVG(actualActiveMillis), 0) FROM session_history")
    suspend fun averageSessionMillis(): Long

    @Query("SELECT COALESCE(MAX(actualActiveMillis), 0) FROM session_history")
    suspend fun longestSessionMillis(): Long
}
