package com.premium.timer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.premium.timer.data.TimerStateEntity
import com.premium.timer.data.UserPreferences
import com.premium.timer.timing.*
import kotlinx.coroutines.launch

private sealed class Screen {
    object Home : Screen()
    object History : Screen()
    object Settings : Screen()
    data class TimerFullscreen(val id: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PremiumTimerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    App()
                }
            }
        }
    }
}

@Composable
private fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { TimerRepository.get(context) }
    val prefs = remember { UserPreferences.get(context) }
    val notifPermission = rememberNotificationPermissionState()
    val scope = rememberCoroutineScope()

    // Reconcile once per process start: recompute correctness against the current
    // elapsedRealtime clock rather than trusting whatever was last displayed.
    LaunchedEffect(Unit) { repository.reconcileAllAfterRestart() }

    val timers by repository.observeAll().collectAsState(initial = emptyList())
    val history by repository.observeHistory().collectAsState(initial = emptyList())
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showAddDialog by remember { mutableStateOf(false) }
    var backgroundEditorFor by remember { mutableStateOf<BackgroundSettings?>(null) }

    // Appearance is loaded FROM the persisted entity the first time we see a given timer id,
    // and any subsequent edit is mirrored back to Room by the LaunchedEffect below — so
    // customization genuinely survives an app restart instead of living only in memory.
    val appearance = remember { mutableStateMapOf<String, BackgroundSettings>() }
    fun appearanceFor(t: TimerStateEntity): BackgroundSettings =
        appearance.getOrPut(t.id) {
            BackgroundSettings(
                initialColor = Color(t.backgroundColorArgb),
                initialAccent = Color(t.accentColorArgb),
                initialType = if (t.backgroundType == "IMAGE") BackgroundType.IMAGE else BackgroundType.COLOR,
                initialImageUri = t.backgroundImageUri?.let { Uri.parse(it) },
                initialVisualStyle = runCatching { VisualStyle.valueOf(t.visualStyle) }.getOrDefault(VisualStyle.DIGITAL),
                initialPrecision = runCatching { DisplayPrecision.valueOf(t.precision) }.getOrDefault(DisplayPrecision.HOURS_MIN_SEC),
                initialFontChoice = runCatching { TimerFontChoice.valueOf(t.fontChoice) }.getOrDefault(TimerFontChoice.MODERN)
            )
        }

    val keepScreenOn by prefs.keepScreenOnFlow.collectAsState(initial = true)

    when (val s = screen) {
        is Screen.Home -> Column(Modifier.fillMaxSize()) {
            if (!notifPermission.isGranted) {
                NotificationPermissionBanner(onRequest = notifPermission.request)
            }
            HomeScreen(
                timers = timers,
                onAddCountdown = { showAddDialog = true },
                onAddStopwatch = {
                    scope.launch {
                        val created = repository.createStopwatch(accent = AccentTeal)
                        screen = Screen.TimerFullscreen(created.id)
                    }
                },
                onOpenTimer = { screen = Screen.TimerFullscreen(it.id) },
                onToggleTimer = { t ->
                    scope.launch {
                        if (t.isFinished) repository.reset(t.id)
                        if (t.isRunning) {
                            repository.pause(t.id)
                        } else {
                            repository.start(t.id)
                            TimerForegroundService.start(context, t.id)
                        }
                    }
                },
                onDeleteTimer = { t -> scope.launch { repository.delete(t.id) } },
                onOpenHistory = { screen = Screen.History },
                onOpenSettings = { screen = Screen.Settings },
                appearanceFor = ::appearanceFor
            )
        }

        is Screen.History -> HistoryScreen(
            sessions = history,
            onClearHistory = { scope.launch { repository.clearHistory() } },
            onBack = { screen = Screen.Home }
        )

        is Screen.Settings -> SettingsScreen(
            keepScreenOn = keepScreenOn,
            onKeepScreenOnChanged = { enabled -> scope.launch { prefs.setKeepScreenOn(enabled) } },
            onResetAppData = {
                scope.launch {
                    repository.resetAllAppData()
                    appearance.clear()
                    screen = Screen.Home
                }
            },
            onBack = { screen = Screen.Home }
        )

        is Screen.TimerFullscreen -> {
            val timer = timers.find { it.id == s.id }
            if (timer == null) {
                screen = Screen.Home
            } else {
                KeepScreenOnWhileVisible(enabled = keepScreenOn)
                val bg = appearanceFor(timer)

                // Mirrors any appearance edit back to Room. Keyed on every field that can
                // change, so an edit to any one of them triggers exactly one persisted write.
                LaunchedEffect(bg.accent, bg.color, bg.visualStyle, bg.type, bg.imageUri, bg.precision, bg.fontChoice) {
                    repository.updateAppearance(
                        id = timer.id,
                        accent = bg.accent,
                        backgroundColor = bg.color,
                        visualStyle = bg.visualStyle,
                        backgroundType = bg.type.name,
                        backgroundImageUri = bg.imageUri?.toString(),
                        clearImage = bg.type == BackgroundType.COLOR,
                        fontChoice = bg.fontChoice
                    )
                    repository.updatePrecision(timer.id, bg.precision)
                }

                // Ticks itself, at the point of use, at a rate matched to the chosen precision
                // (fast for decimals, calmer for plain HH:MM:SS) so the number actually advances
                // smoothly instead of freezing or stuttering.
                val now = rememberTickingNow(tickIntervalFor(bg.precision))
                val currentMillis = if (timer.mode() == TimerMode.COUNTDOWN)
                    TimeEngine.remainingMillis(timer, now) else TimeEngine.activeElapsedMillis(timer, now)

                FullscreenDisplay(
                    title = timer.name,
                    currentMillis = currentMillis,
                    totalMillis = timer.totalMillis,
                    isRunning = timer.isRunning,
                    background = bg,
                    onToggleRun = {
                        scope.launch {
                            if (timer.isFinished) repository.reset(timer.id)
                            if (timer.isRunning) {
                                repository.pause(timer.id)
                            } else {
                                repository.start(timer.id)
                                TimerForegroundService.start(context, timer.id)
                            }
                        }
                    },
                    onReset = { scope.launch { repository.reset(timer.id) } },
                    onAddTime = if (timer.mode() == TimerMode.COUNTDOWN) { millis ->
                        scope.launch { repository.addTime(timer.id, millis) }
                    } else null,
                    onFinishEarly = if (timer.mode() == TimerMode.COUNTDOWN && timer.isRunning) {
                        { scope.launch { repository.finishEarly(timer.id) } }
                    } else null,
                    onEditBackground = { backgroundEditorFor = bg },
                    onClose = { screen = Screen.Home }
                )
            }
        }
    }

    if (showAddDialog) {
        AddTimerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, total, project, tags ->
                scope.launch {
                    val created = repository.createCountdown(name, total, AccentPurple, project, tags)
                    showAddDialog = false
                    screen = Screen.TimerFullscreen(created.id)
                }
            }
        )
    }

    backgroundEditorFor?.let { settings ->
        BackgroundPickerSheet(
            settings = settings,
            onDismiss = { backgroundEditorFor = null }
        )
    }
}

@Composable
private fun NotificationPermissionBanner(onRequest: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF2D1B00)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Notifications are off, so running timers won't show progress in your notification shade. Timing itself still stays accurate.",
            color = Color(0xFFFFD59E),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text("Enable", color = AccentAmber, modifier = Modifier.clickable { onRequest() })
    }
}
