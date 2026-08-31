package com.premium.timer.timing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

/**
 * Section 2 requires: "Deny notification permission: app must clearly explain the limitation
 * without crashing." This does exactly that — it never blocks timer functionality, it only
 * tracks whether we're allowed to show the persistent progress notification, and exposes a
 * flag the UI can use to show a small, honest banner instead of silently doing nothing.
 */
@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = androidx.compose.ui.platform.LocalContext.current
    var granted by remember { mutableStateOf(hasNotificationPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    return remember {
        NotificationPermissionState(
            isGrantedProvider = { granted },
            request = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    granted = true // no runtime permission needed pre-Android 13
                }
            }
        )
    }
}

class NotificationPermissionState(
    private val isGrantedProvider: () -> Boolean,
    val request: () -> Unit
) {
    val isGranted: Boolean get() = isGrantedProvider()
}

fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
