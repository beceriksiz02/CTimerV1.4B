package com.premium.timer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerStateDao {

    @Upsert
    suspend fun upsert(state: TimerStateEntity)

    @Delete
    suspend fun delete(state: TimerStateEntity)

    @Query("DELETE FROM timer_state WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM timer_state ORDER BY lastPersistedAtWallClock DESC")
    fun observeAll(): Flow<List<TimerStateEntity>>

    @Query("SELECT * FROM timer_state ORDER BY lastPersistedAtWallClock DESC")
    suspend fun getAllOnce(): List<TimerStateEntity>

    @Query("SELECT * FROM timer_state WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TimerStateEntity?

    @Query("SELECT * FROM timer_state WHERE isRunning = 1")
    suspend fun getAllRunning(): List<TimerStateEntity>
}
