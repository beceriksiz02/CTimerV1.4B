package com.premium.timer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per finished/stopped session, for both countdown and stopwatch — the spec is explicit
 * that history must not treat the stopwatch as a lesser feature, so both write here identically.
 */
@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey val id: String,
    val timerName: String,
    val mode: String,                 // "COUNTDOWN" or "STOPWATCH"
    val startWallClock: Long,
    val endWallClock: Long,
    val plannedMillis: Long,          // configured duration; 0 for stopwatch
    val actualActiveMillis: Long,     // actual active (non-paused) time achieved
    val completed: Boolean,           // true = countdown reached zero / stopwatch stopped normally;
                                       // false = abandoned (deleted/reset while still short of target)
    val project: String? = null,
    val tags: String? = null
)
