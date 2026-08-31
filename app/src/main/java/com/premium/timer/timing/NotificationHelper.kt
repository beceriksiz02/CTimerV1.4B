package com.premium.timer.timing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.premium.timer.MainActivity
import com.premium.timer.data.TimerStateEntity

object NotificationHelper {
    const val CHANNEL_ID = "active_timers"
    const val ACTION_PAUSE = "com.premium.timer.action.PAUSE"
    const val ACTION_RESUME = "com.premium.timer.action.RESUME"
    const val ACTION_STOP = "com.premium.timer.action.STOP"
    const val ACTION_ADD_TIME = "com.premium.timer.action.ADD_TIME"
    const val EXTRA_TIMER_ID = "timer_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active timers",
                NotificationManager.IMPORTANCE_LOW // low = no sound/heads-up spam while ticking
            ).apply {
                description = "Shows the countdown or stopwatch currently running"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun build(context: Context, state: TimerStateEntity): android.app.Notification {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context, state.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = if (state.mode() == TimerMode.COUNTDOWN) {
            formatMillis(TimeEngine.remainingMillis(state))
        } else {
            formatMillis(TimeEngine.activeElapsedMillis(state))
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(state.name)
            .setContentText(timeText + if (state.isRunning) "" else " · paused")
            .setOngoing(state.isRunning)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (state.isRunning) {
            builder.addAction(0, "Pause", actionIntent(context, ACTION_PAUSE, state.id))
        } else if (!state.isFinished) {
            builder.addAction(0, "Resume", actionIntent(context, ACTION_RESUME, state.id))
        }
        if (state.mode() == TimerMode.COUNTDOWN && !state.isFinished) {
            builder.addAction(0, "+1 min", actionIntent(context, ACTION_ADD_TIME, state.id))
        }
        builder.addAction(0, "Stop", actionIntent(context, ACTION_STOP, state.id))

        return builder.build()
    }

    private fun actionIntent(context: Context, action: String, timerId: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getBroadcast(
            context, (action + timerId).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun formatMillis(millis: Long): String {
        val totalSeconds = millis / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
