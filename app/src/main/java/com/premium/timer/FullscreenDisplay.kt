@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.premium.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.premium.timer.timing.DisplayPrecision
import com.premium.timer.timing.VisualStyle
import com.premium.timer.timing.formatWithPrecision

/** Kept for any legacy call site expecting a plain HH:MM:SS/MM:SS string. */
fun formatHms(millis: Long): String = formatWithPrecision(millis, DisplayPrecision.HOURS_MIN_SEC)

/**
 * Full-screen presentation for a running timer or the stopwatch.
 * @param totalMillis 0 means "count up" (stopwatch mode); otherwise counts down from this value.
 */
@Composable
fun FullscreenDisplay(
    title: String,
    currentMillis: Long,
    totalMillis: Long,
    isRunning: Boolean,
    background: BackgroundSettings,
    onToggleRun: () -> Unit,
    onReset: () -> Unit,
    onAddTime: ((Long) -> Unit)? = null,   // null hides the add-time row (e.g. for stopwatch)
    onFinishEarly: (() -> Unit)? = null,   // null hides the finish-early action
    onEditBackground: () -> Unit,
    onClose: () -> Unit,
    extraAction: (@Composable () -> Unit)? = null
) {
    Box(Modifier.fillMaxSize()) {
        // Background layer
        when (background.type) {
            BackgroundType.IMAGE -> if (background.imageUri != null) {
                AsyncImage(
                    model = background.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
            } else {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
            BackgroundType.COLOR -> Box(Modifier.fillMaxSize().background(background.color))
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPaddingSafe().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            if (background.visualStyle != VisualStyle.MINIMAL) {
                Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Row {
                IconButton(onClick = onEditBackground) {
                    Icon(Icons.Filled.Palette, contentDescription = "Customize", tint = Color.White)
                }
            }
        }

        // Center content — cycles through the 5 visual styles
        Box(
            Modifier.fillMaxSize().padding(32.dp)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    cyclePrecision(background)
                },
            contentAlignment = Alignment.Center
        ) {
            val label = formatWithPrecision(currentMillis, background.precision)
            val fraction = if (totalMillis > 0) {
                (currentMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
            } else {
                ((currentMillis % 60000L).toFloat() / 60000f)
            }

            when (background.visualStyle) {
                VisualStyle.ANALOG -> AnalogClockFace(
                    progressFraction = fraction, accent = background.accent, centerLabel = label,
                    modifier = Modifier.fillMaxWidth(0.82f)
                )
                VisualStyle.THICK_DISK -> ThickDiskFace(
                    progressFraction = fraction, accent = background.accent, centerLabel = label,
                    modifier = Modifier.fillMaxWidth(0.82f)
                )
                VisualStyle.SEGMENTED_RING -> SegmentedRingFace(
                    progressFraction = fraction, accent = background.accent, centerLabel = label,
                    modifier = Modifier.fillMaxWidth(0.82f)
                )
                VisualStyle.MINIMAL -> Text(
                    text = label, color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Thin,
                    fontFamily = background.fontChoice.family
                )
                VisualStyle.DIGITAL -> Text(
                    text = label, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light,
                    fontFamily = background.fontChoice.family
                )
            }
        }

        // Bottom controls
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onAddTime != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(1L to "+1m", 5L to "+5m", 10L to "+10m").forEach { (mins, label) ->
                        OutlinedButton(onClick = { onAddTime(mins * 60_000L) }) {
                            Text(label, color = Color.White)
                        }
                    }
                    if (onFinishEarly != null) {
                        OutlinedButton(onClick = onFinishEarly) {
                            Text("Finish", color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(onClick = onReset, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset", tint = Color.White)
                }
                Spacer(Modifier.width(24.dp))
                FilledIconButton(
                    onClick = onToggleRun,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = background.accent)
                ) {
                    Icon(
                        if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(36.dp)
                    )
                }
                if (extraAction != null) {
                    Spacer(Modifier.width(24.dp))
                    extraAction()
                }
            }
        }

        // Small precision hint, bottom-left, so the tap-to-cycle affordance is discoverable
        Text(
            text = background.precision.label,
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp)
        )
    }
}

private fun cyclePrecision(background: BackgroundSettings) {
    val values = DisplayPrecision.values()
    val nextIndex = (values.indexOf(background.precision) + 1) % values.size
    background.precision = values[nextIndex]
}

@Composable
fun Modifier.statusBarsPaddingSafe(): Modifier = this.padding(top = 12.dp)
