package com.premium.timer.timing

import android.os.SystemClock
import com.premium.timer.data.TimerStateEntity

enum class TimerMode { COUNTDOWN, STOPWATCH }

/**
 * All timing math lives here, in one place, with zero Android UI dependencies, so it can't
 * drift depending on where it's called from (Activity, Service, notification updater, etc).
 *
 * The rule that makes this reliable: we never trust a previously-displayed number. We always
 * recompute from (accumulatedActiveMillis, resumedAtElapsedRealtime, isRunning) against the
 * CURRENT elapsedRealtime(). elapsedRealtime() keeps advancing through sleep/Doze and is not
 * affected by the user changing the wall clock or time zone, which is exactly what section 2
 * of the requirements demands.
 */
object TimeEngine {

    /** Milliseconds of active (non-paused) time elapsed for this session, right now. */
    fun activeElapsedMillis(state: TimerStateEntity, nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Long {
        return if (state.isRunning) {
            val currentSegment = (nowElapsedRealtime - state.resumedAtElapsedRealtime).coerceAtLeast(0)
            state.accumulatedActiveMillis + currentSegment
        } else {
            state.accumulatedActiveMillis
        }
    }

    /** For countdown timers: remaining time right now, floored at 0. */
    fun remainingMillis(state: TimerStateEntity, nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Long {
        val elapsed = activeElapsedMillis(state, nowElapsedRealtime)
        return (state.totalMillis - elapsed).coerceAtLeast(0)
    }

    fun isNowFinished(state: TimerStateEntity, nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Boolean {
        if (state.mode() != TimerMode.COUNTDOWN) return false
        return remainingMillis(state, nowElapsedRealtime) <= 0L
    }

    /** Transition helpers — each returns a NEW entity; callers must persist it immediately. */

    fun start(state: TimerStateEntity, now: Long = SystemClock.elapsedRealtime(), wallNow: Long = System.currentTimeMillis()): TimerStateEntity =
        state.copy(
            isRunning = true,
            isFinished = false,
            resumedAtElapsedRealtime = now,
            resumedAtWallClock = wallNow,
            lastPersistedAtWallClock = wallNow
        )

    fun pause(state: TimerStateEntity, now: Long = SystemClock.elapsedRealtime(), wallNow: Long = System.currentTimeMillis()): TimerStateEntity {
        if (!state.isRunning) return state.copy(lastPersistedAtWallClock = wallNow)
        val newAccumulated = activeElapsedMillis(state, now)
        return state.copy(
            isRunning = false,
            accumulatedActiveMillis = newAccumulated,
            lastPersistedAtWallClock = wallNow
        )
    }

    fun reset(state: TimerStateEntity, wallNow: Long = System.currentTimeMillis()): TimerStateEntity =
        state.copy(
            isRunning = false,
            isFinished = false,
            accumulatedActiveMillis = 0L,
            resumedAtElapsedRealtime = 0L,
            resumedAtWallClock = wallNow,
            lastPersistedAtWallClock = wallNow
        )

    fun addTime(state: TimerStateEntity, deltaMillis: Long, wallNow: Long = System.currentTimeMillis()): TimerStateEntity =
        state.copy(
            totalMillis = (state.totalMillis + deltaMillis).coerceAtLeast(0),
            isFinished = false,
            lastPersistedAtWallClock = wallNow
        )

    fun markFinished(state: TimerStateEntity, wallNow: Long = System.currentTimeMillis()): TimerStateEntity =
        state.copy(isRunning = false, isFinished = true, lastPersistedAtWallClock = wallNow)

    /**
     * Called once, right after process/app start (Activity onCreate, Service onCreate, or boot
     * receiver), to reconcile persisted state against the CURRENT elapsedRealtime clock.
     *
     * Why this is necessary: resumedAtElapsedRealtime was captured against the PREVIOUS boot's
     * elapsedRealtime clock, which resets to 0 on every reboot. If we detect the device rebooted
     * since the timer was last running, we can't know exactly how much active time passed during
     * the outage, so we conservatively pause the session at its last known accumulated value
     * rather than guessing — silently fabricating elapsed time would violate priority #1
     * (trustworthy timing) worse than asking the user to resume manually.
     */
    fun reconcileAfterProcessOrBoot(
        state: TimerStateEntity,
        nowElapsedRealtime: Long = SystemClock.elapsedRealtime(),
        nowWallClock: Long = System.currentTimeMillis()
    ): TimerStateEntity {
        if (!state.isRunning) return state

        val rebooted = state.resumedAtElapsedRealtime > nowElapsedRealtime
        return if (rebooted) {
            // Can't trust elapsedRealtime across the reboot gap - stop the clock safely.
            state.copy(isRunning = false, lastPersistedAtWallClock = nowWallClock)
        } else {
            // Same boot session (e.g. process was killed and restarted by Android, or app was
            // just reopened) - elapsedRealtime is still valid, so the running state is still
            // correct as-is. Just refresh the persisted-at marker.
            state.copy(lastPersistedAtWallClock = nowWallClock)
        }
    }
}

fun TimerStateEntity.mode(): TimerMode = TimerMode.valueOf(mode)
