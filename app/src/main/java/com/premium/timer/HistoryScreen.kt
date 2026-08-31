@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.premium.timer

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.premium.timer.data.SessionHistoryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Honest, simple metrics — no fabricated "productivity score", per the spec. Just what
 * actually happened: when, how long, planned vs actual, completed vs abandoned.
 */
@Composable
fun HistoryScreen(
    sessions: List<SessionHistoryEntity>,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val startOfToday = startOfDay(now)
    val startOfWeek = startOfToday - TimeUnit.DAYS.toMillis(6)
    val startOfMonth = startOfToday - TimeUnit.DAYS.toMillis(29)

    val totalToday = sessions.filter { it.startWallClock >= startOfToday }.sumOf { it.actualActiveMillis }
    val totalWeek = sessions.filter { it.startWallClock >= startOfWeek }.sumOf { it.actualActiveMillis }
    val totalMonth = sessions.filter { it.startWallClock >= startOfMonth }.sumOf { it.actualActiveMillis }
    val completed = sessions.count { it.completed }
    val abandoned = sessions.size - completed
    val average = if (sessions.isNotEmpty()) sessions.sumOf { it.actualActiveMillis } / sessions.size else 0L
    val longest = sessions.maxOfOrNull { it.actualActiveMillis } ?: 0L

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (sessions.isNotEmpty()) {
                        IconButton(onClick = { exportCsv(context, sessions) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Export CSV", tint = Color.White)
                        }
                        IconButton(onClick = onClearHistory) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear history", tint = TextMuted)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    StatCard("Today", formatHms(totalToday))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { StatCard("This week", formatHms(totalWeek), compact = true) }
                        Box(Modifier.weight(1f)) { StatCard("This month", formatHms(totalMonth), compact = true) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { StatCard("Completed", "$completed", compact = true) }
                        Box(Modifier.weight(1f)) { StatCard("Stopped early", "$abandoned", compact = true) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { StatCard("Avg session", formatHms(average), compact = true) }
                        Box(Modifier.weight(1f)) { StatCard("Longest", formatHms(longest), compact = true) }
                    }
                }
            }

            if (sessions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("No sessions yet — completed or stopped timers will show up here", color = TextMuted)
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) { SessionRow(session) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, compact: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(if (compact) 14.dp else 18.dp)) {
            Text(label, color = TextMuted, fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = if (compact) 20.sp else 28.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SessionRow(session: SessionHistoryEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(session.timerName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(
                    dateLabel(session.startWallClock) + " · " + (if (session.mode == "STOPWATCH") "Stopwatch" else "Countdown"),
                    color = TextMuted, fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatHms(session.actualActiveMillis), color = Color.White, fontSize = 15.sp)
                Text(
                    if (session.completed) "Completed" else "Stopped early",
                    color = if (session.completed) AccentTeal else TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun startOfDay(wallClockMillis: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = wallClockMillis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun dateLabel(wallClockMillis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(wallClockMillis))

/** Exports session history as a CSV file and hands it to the system share sheet. */
private fun exportCsv(context: android.content.Context, sessions: List<SessionHistoryEntity>) {
    val header = "name,mode,start,end,planned_ms,actual_ms,completed\n"
    val rows = sessions.joinToString("\n") { s ->
        "\"${s.timerName.replace("\"", "'")}\",${s.mode},${s.startWallClock},${s.endWallClock},${s.plannedMillis},${s.actualActiveMillis},${s.completed}"
    }
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "premium_timer_history.csv")
    file.writeText(header + rows)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export session history"))
}
