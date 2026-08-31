@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.premium.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premium.timer.data.TimerStateEntity
import com.premium.timer.timing.TimeEngine
import com.premium.timer.timing.TimerMode
import com.premium.timer.timing.mode
import com.premium.timer.timing.rememberTickingNow
import com.premium.timer.timing.tickIntervalFor

@Composable
fun HomeScreen(
    timers: List<TimerStateEntity>,
    onAddCountdown: () -> Unit,
    onAddStopwatch: () -> Unit,
    onOpenTimer: (TimerStateEntity) -> Unit,
    onToggleTimer: (TimerStateEntity) -> Unit,
    onDeleteTimer: (TimerStateEntity) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    appearanceFor: (TimerStateEntity) -> BackgroundSettings
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Premium Timer", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History", tint = Color.White)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCountdown, containerColor = AccentPurple) {
                Icon(Icons.Filled.Add, contentDescription = "Add timer", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                StopwatchLauncherCard(onClick = onAddStopwatch)
            }

            val countdowns = timers.filter { it.mode() == TimerMode.COUNTDOWN }
            val stopwatches = timers.filter { it.mode() == TimerMode.STOPWATCH }

            if (countdowns.isEmpty() && stopwatches.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No timers yet — tap + to create one", color = TextMuted)
                    }
                }
            }

            items(stopwatches, key = { it.id }) { t ->
                TimerCard(t, appearanceFor(t), onOpen = { onOpenTimer(t) }, onToggle = { onToggleTimer(t) }, onDelete = { onDeleteTimer(t) })
            }

            items(countdowns, key = { it.id }) { t ->
                TimerCard(t, appearanceFor(t), onOpen = { onOpenTimer(t) }, onToggle = { onToggleTimer(t) }, onDelete = { onDeleteTimer(t) })
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StopwatchLauncherCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(AccentTeal.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Watch, contentDescription = null, tint = AccentTeal)
                }
                Spacer(Modifier.width(14.dp))
                Text("New stopwatch", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
            Icon(Icons.Filled.Add, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
private fun TimerCard(
    timer: TimerStateEntity,
    appearance: BackgroundSettings,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    // Subscribed here, directly at the point of use, so this card keeps recomposing every
    // tick regardless of what Compose decides to skip further up the tree. Tick speed matches
    // the timer's own precision setting (fast for decimals, calmer for plain HH:MM:SS).
    val now = rememberTickingNow(tickIntervalFor(appearance.precision))
    val displayMillis = if (timer.mode() == TimerMode.COUNTDOWN)
        TimeEngine.remainingMillis(timer, now) else TimeEngine.activeElapsedMillis(timer, now)

    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(appearance.accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (timer.mode() == TimerMode.STOPWATCH) Icons.Filled.Watch else Icons.Filled.Timer,
                        contentDescription = null,
                        tint = appearance.accent
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(timer.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text(
                        formatHms(displayMillis) + when {
                            timer.isFinished -> " · done"
                            !timer.isRunning -> " · paused"
                            else -> ""
                        },
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle",
                        tint = appearance.accent
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextMuted)
                }
            }
        }
    }
}
