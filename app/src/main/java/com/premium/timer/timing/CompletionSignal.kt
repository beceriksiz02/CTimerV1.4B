package com.premium.timer.timing

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object CompletionSignal {
    /**
     * Fires vibration + a system alert sound directly (not routed through a notification),
     * so completion is still felt/heard even if the user denied POST_NOTIFICATIONS — that
     * permission only blocks the persistent progress notification, not this.
     */
    fun fire(context: Context, timerName: String) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 300, 150, 300), -1)
                }
            }
        } catch (_: Exception) {
            // Vibration is a nicety, never worth crashing the timer over.
        }

        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.play()
        } catch (_: Exception) {
            // Same reasoning - never let a missing ringtone crash timer completion.
        }
    }
}
