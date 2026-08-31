package com.premium.timer.timing

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Applies FLAG_KEEP_SCREEN_ON only while this composable is part of the composition (i.e. only
 * while the timer/stopwatch fullscreen is actually visible), and removes it in every disposal
 * path (navigating away, activity destroyed, etc) so it can never leak and drain battery once
 * the user leaves the screen. This is a window flag, NOT a WakeLock — no special permission
 * needed, and Android automatically drops it the moment the Activity stops.
 *
 * enabled = false is a no-op, satisfying the "let the user disable this" requirement.
 */
@Composable
fun KeepScreenOnWhileVisible(enabled: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(enabled) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
