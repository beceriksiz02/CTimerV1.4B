package com.premium.timer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single source of truth for "what is this timer/stopwatch actually doing right now".
 *
 * Design principle (critical for correctness): we NEVER persist a live "remaining/elapsed"
 * number that gets decremented by a UI loop. Instead we persist the inputs needed to
 * RECOMPUTE the correct value at any instant:
 *
 *   remaining = totalMillis - (accumulatedActiveMillis + (now - resumedAtElapsedRealtime, if running))
 *   elapsed   =                accumulatedActiveMillis + (now - resumedAtElapsedRealtime, if running)
 *
 * We use SystemClock.elapsedRealtime() (monotonic since boot, unaffected by wall-clock/time-zone
 * changes and kept running through Doze) as the "now" reference while the process is alive, and
 * additionally store a wall-clock timestamp so we can sanity-check across reboots (elapsedRealtime
 * resets to 0 on reboot, wall clock does not).
 */
@Entity(tableName = "timer_state")
data class TimerStateEntity(
    @PrimaryKey val id: String,

    val name: String,
    val mode: String,               // "COUNTDOWN" or "STOPWATCH"
    val totalMillis: Long,          // configured duration; 0 for stopwatch (counts up, no ceiling)

    val isRunning: Boolean,
    val isFinished: Boolean,

    // Time accounted for BEFORE the current run segment (i.e. all previous run segments summed).
    val accumulatedActiveMillis: Long,

    // elapsedRealtime() at the moment the current run segment started/resumed.
    // Meaningless/ignored if isRunning == false.
    val resumedAtElapsedRealtime: Long,

    // Wall-clock (System.currentTimeMillis()) at the same instant as resumedAtElapsedRealtime.
    // Used ONLY to detect "the device rebooted since this was running" (elapsedRealtime resets
    // to 0 on reboot, so a resumedAtElapsedRealtime that's now "in the future" relative to the
    // fresh boot-time elapsedRealtime tells us the session must be treated as paused-at-recovery).
    val resumedAtWallClock: Long,

    val lastPersistedAtWallClock: Long,

    val accentColorArgb: Int,
    val backgroundColorArgb: Int,

    // Added in schema v2 (Phase 2/3): persisted appearance + visual style, so customization
    // actually survives an app restart instead of living only in memory.
    val visualStyle: String = "DIGITAL",       // one of VisualStyle enum names
    val backgroundType: String = "COLOR",      // "COLOR" or "IMAGE"
    val backgroundImageUri: String? = null,

    // Added in schema v2: display precision, per-timer.
    val precision: String = "HMS",              // one of DisplayPrecision enum names

    // Added in schema v3 (Phase 4): typography choice, per-timer.
    val fontChoice: String = "MODERN",          // one of TimerFontChoice enum names

    // Added in schema v3 (Phase 4): lightweight productivity workflow fields.
    val project: String? = null,
    val tags: String? = null                    // comma-separated, kept simple deliberately
)
