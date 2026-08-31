package com.premium.timer.timing

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.premium.timer.data.AppDatabase
import com.premium.timer.data.SessionHistoryEntity
import com.premium.timer.data.TimerStateEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Everything that touches persisted timer state goes through here — the UI, the foreground
 * service, the notification action receiver, and the boot receiver all call the same methods,
 * so there is exactly one code path that can mutate a timer's stored state.
 */
class TimerRepository private constructor(context: Context) {
    private val db = AppDatabase.get(context)
    private val dao = db.timerStateDao()
    private val historyDao = db.sessionHistoryDao()

    fun observeAll(): Flow<List<TimerStateEntity>> = dao.observeAll()
    fun observeHistory(): Flow<List<SessionHistoryEntity>> = historyDao.observeAll()

    suspend fun getAllOnce() = dao.getAllOnce()
    suspend fun getAllRunning() = dao.getAllRunning()
    suspend fun get(id: String) = dao.getById(id)
    suspend fun clearHistory() = historyDao.clearAll()
    suspend fun totalActiveMillisSince(sinceWallClock: Long) = historyDao.totalActiveMillisSince(sinceWallClock)
    suspend fun completedCount() = historyDao.completedCount()
    suspend fun abandonedCount() = historyDao.abandonedCount()
    suspend fun averageSessionMillis() = historyDao.averageSessionMillis()
    suspend fun longestSessionMillis() = historyDao.longestSessionMillis()

    /** Full wipe: every timer, every session record. Used by the "Reset app data" action,
     *  which the UI gates behind an explicit confirmation dialog before ever calling this. */
    suspend fun resetAllAppData() {
        getAllOnce().forEach { dao.deleteById(it.id) }
        historyDao.clearAll()
    }

    suspend fun createCountdown(
        name: String, totalMillis: Long, accent: Color,
        project: String? = null, tags: String? = null
    ): TimerStateEntity {
        val now = System.currentTimeMillis()
        val entity = TimerStateEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            mode = TimerMode.COUNTDOWN.name,
            totalMillis = totalMillis,
            isRunning = false,
            isFinished = false,
            accumulatedActiveMillis = 0L,
            resumedAtElapsedRealtime = 0L,
            resumedAtWallClock = now,
            lastPersistedAtWallClock = now,
            accentColorArgb = accent.toArgb(),
            backgroundColorArgb = Color.Black.toArgb(),
            visualStyle = VisualStyle.DIGITAL.name,
            backgroundType = "COLOR",
            backgroundImageUri = null,
            precision = DisplayPrecision.HOURS_MIN_SEC.name,
            fontChoice = TimerFontChoice.MODERN.name,
            project = project?.ifBlank { null },
            tags = tags?.ifBlank { null }
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun createStopwatch(name: String = "Stopwatch", accent: Color): TimerStateEntity {
        val now = System.currentTimeMillis()
        val entity = TimerStateEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            mode = TimerMode.STOPWATCH.name,
            totalMillis = 0L,
            isRunning = false,
            isFinished = false,
            accumulatedActiveMillis = 0L,
            resumedAtElapsedRealtime = 0L,
            resumedAtWallClock = now,
            lastPersistedAtWallClock = now,
            accentColorArgb = accent.toArgb(),
            backgroundColorArgb = Color.Black.toArgb(),
            visualStyle = VisualStyle.DIGITAL.name,
            backgroundType = "COLOR",
            backgroundImageUri = null,
            precision = DisplayPrecision.HOURS_MIN_SEC.name,
            fontChoice = TimerFontChoice.MODERN.name
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun start(id: String) = mutate(id) { TimeEngine.start(it) }

    suspend fun pause(id: String) = mutate(id) { TimeEngine.pause(it) }

    /** Reset also logs an "abandoned" history row if meaningful progress had been made. */
    suspend fun reset(id: String) {
        val before = dao.getById(id) ?: return
        recordHistoryIfMeaningful(before, completed = false)
        dao.upsert(TimeEngine.reset(before))
    }

    suspend fun addTime(id: String, deltaMillis: Long) = mutate(id) { TimeEngine.addTime(it, deltaMillis) }

    /** User explicitly ends a countdown before it reaches zero, but wants it logged as a
     *  deliberate finish rather than an abandoned session. */
    suspend fun finishEarly(id: String) {
        val before = dao.getById(id) ?: return
        val paused = TimeEngine.pause(before)
        dao.upsert(paused.copy(isFinished = true))
        recordHistoryIfMeaningful(paused, completed = true)
    }

    /** Completion always logs a finished history row. */
    suspend fun markFinished(id: String) {
        val before = dao.getById(id) ?: return
        val finished = TimeEngine.markFinished(before)
        dao.upsert(finished)
        recordHistoryIfMeaningful(finished, completed = true)
    }

    suspend fun delete(id: String) {
        val before = dao.getById(id)
        if (before != null) recordHistoryIfMeaningful(before, completed = before.isFinished)
        dao.deleteById(id)
    }

    suspend fun updateAppearance(
        id: String,
        accent: Color? = null,
        backgroundColor: Color? = null,
        visualStyle: VisualStyle? = null,
        backgroundType: String? = null,
        backgroundImageUri: String? = null,
        clearImage: Boolean = false,
        fontChoice: TimerFontChoice? = null
    ) = mutate(id) { current ->
        current.copy(
            accentColorArgb = accent?.toArgb() ?: current.accentColorArgb,
            backgroundColorArgb = backgroundColor?.toArgb() ?: current.backgroundColorArgb,
            visualStyle = visualStyle?.name ?: current.visualStyle,
            backgroundType = backgroundType ?: current.backgroundType,
            backgroundImageUri = if (clearImage) null else (backgroundImageUri ?: current.backgroundImageUri),
            fontChoice = fontChoice?.name ?: current.fontChoice
        )
    }

    suspend fun updatePrecision(id: String, precision: DisplayPrecision) =
        mutate(id) { it.copy(precision = precision.name) }

    suspend fun reconcileAllAfterRestart() {
        dao.getAllRunning().forEach { state ->
            dao.upsert(TimeEngine.reconcileAfterProcessOrBoot(state))
        }
    }

    private suspend fun recordHistoryIfMeaningful(state: TimerStateEntity, completed: Boolean) {
        val activeMillis = TimeEngine.activeElapsedMillis(state)
        // Don't clutter history with accidental taps under 3 seconds of activity.
        if (activeMillis < 3000L) return
        historyDao.insert(
            SessionHistoryEntity(
                id = UUID.randomUUID().toString(),
                timerName = state.name,
                mode = state.mode,
                startWallClock = state.resumedAtWallClock.takeIf { it > 0 } ?: state.lastPersistedAtWallClock,
                endWallClock = System.currentTimeMillis(),
                plannedMillis = state.totalMillis,
                actualActiveMillis = activeMillis,
                completed = completed,
                project = state.project,
                tags = state.tags
            )
        )
    }

    private suspend fun mutate(id: String, transform: (TimerStateEntity) -> TimerStateEntity) {
        val current = dao.getById(id) ?: return
        dao.upsert(transform(current))
    }

    companion object {
        @Volatile private var instance: TimerRepository? = null
        fun get(context: Context): TimerRepository =
            instance ?: synchronized(this) {
                instance ?: TimerRepository(context.applicationContext).also { instance = it }
            }
    }
}
