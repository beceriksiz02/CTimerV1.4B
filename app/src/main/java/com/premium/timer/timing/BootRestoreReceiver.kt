package com.premium.timer.timing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * elapsedRealtime() resets to 0 on every reboot, so any timer that was "running" before the
 * reboot can no longer have its elapsed time trusted across the outage. We deliberately do NOT
 * try to guess how much time passed — see TimeEngine.reconcileAfterProcessOrBoot for the reasoning.
 * We just make sure the persisted state is safely marked paused-at-last-known-value so the next
 * time the user opens the app they see something correct and can resume deliberately.
 */
class BootRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val repository = TimerRepository.get(context)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                repository.reconcileAllAfterRestart()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
