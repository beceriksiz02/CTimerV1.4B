package com.premium.timer.timing

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs only while at least one timer/stopwatch is actively running. It does not do the timing
 * math itself in any way that matters — TimeEngine + the persisted TimerStateEntity are the
 * source of truth — this service's only two jobs are:
 *   1. Hold a foreground-service notification so Android doesn't freeze/kill the process,
 *      keeping the CPU able to actually check elapsedRealtime and hit completion accurately.
 *   2. Refresh that notification's displayed text periodically and detect countdown completion.
 *
 * If this service is killed anyway (extreme memory pressure, user force-stop), no time is lost:
 * the next time the app/service starts, TimeEngine recomputes the correct value from the
 * persisted timestamps rather than from anything this service was holding in memory.
 */
class TimerForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var repository: TimerRepository

    override fun onCreate() {
        super.onCreate()
        repository = TimerRepository.get(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getStringExtra(NotificationHelper.EXTRA_TIMER_ID)
        if (timerId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            watch(timerId)
        }

        // START_STICKY: if the system kills this service under memory pressure while a timer
        // is running, ask Android to restart it. On restart intent will be null, so we fall back
        // to re-checking the database for any still-running timer rather than trusting the
        // original intent extras.
        return START_STICKY
    }

    private suspend fun watch(initialTimerId: String) {
        var currentId: String? = initialTimerId
        while (currentId != null) {
            val state = repository.get(currentId)
            if (state == null || !state.isRunning) {
                // Nothing left to actively watch — check if any OTHER timer is still running
                // (e.g. user started a second one) before shutting the service down.
                val stillRunning = repository.getAllRunning().firstOrNull()
                if (stillRunning == null) {
                    stopForegroundCompat()
                    stopSelf()
                    return
                }
                currentId = stillRunning.id
                continue
            }

            if (TimeEngine.isNowFinished(state)) {
                repository.markFinished(state.id)
                val finished = repository.get(state.id)
                if (finished != null) {
                    postNotification(finished)
                }
                CompletionSignal.fire(applicationContext, state.name)
                // keep loop alive briefly in case another timer is running
                val stillRunning = repository.getAllRunning().firstOrNull()
                if (stillRunning == null) {
                    stopForegroundCompat()
                    stopSelf()
                    return
                }
                currentId = stillRunning.id
                continue
            }

            postNotification(state)
            delay(1000L)
        }
    }

    private fun postNotification(state: com.premium.timer.data.TimerStateEntity) {
        val notification = NotificationHelper.build(applicationContext, state)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, Service.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 4201

        fun start(context: android.content.Context, timerId: String) {
            val intent = Intent(context, TimerForegroundService::class.java)
                .putExtra(NotificationHelper.EXTRA_TIMER_ID, timerId)
            context.startForegroundService(intent)
        }
    }
}
