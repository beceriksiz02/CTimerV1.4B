package com.premium.timer.timing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Notification action buttons must work even if MainActivity/the Compose UI isn't alive — this
 * receiver talks directly to TimerRepository (Room), the same single source of truth the UI and
 * the service use, so a "Pause" tap from the notification is exactly as durable as pausing from
 * inside the app.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(NotificationHelper.EXTRA_TIMER_ID) ?: return
        val repository = TimerRepository.get(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_PAUSE -> repository.pause(timerId)
                    NotificationHelper.ACTION_RESUME -> {
                        repository.start(timerId)
                        TimerForegroundService.start(context, timerId)
                    }
                    NotificationHelper.ACTION_ADD_TIME -> repository.addTime(timerId, 60_000L)
                    NotificationHelper.ACTION_STOP -> {
                        repository.pause(timerId)
                        NotificationManagerCompat.from(context).cancel(TimerForegroundService.NOTIFICATION_ID)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
