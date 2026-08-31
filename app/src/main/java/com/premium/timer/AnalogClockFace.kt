package com.premium.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Round countdown face: a sweeping progress ring plus 60 tick indicators (like a clock),
 * with the remaining time printed in the center.
 */
@Composable
fun AnalogClockFace(
    progressFraction: Float, // 0f (empty) .. 1f (full)
    accent: Color,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.045f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

            // background track
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // tick marks (60 total, thicker every 5th - like a real clock)
            for (i in 0 until 60) {
                val angle = Math.toRadians((i * 6.0) - 90.0)
                val isMajor = i % 5 == 0
                val outerR = radius + strokeWidth * 0.9f
                val innerR = outerR - if (isMajor) strokeWidth * 0.9f else strokeWidth * 0.45f
                val x1 = center.x + (cos(angle) * outerR).toFloat()
                val y1 = center.y + (sin(angle) * outerR).toFloat()
                val x2 = center.x + (cos(angle) * innerR).toFloat()
                val y2 = center.y + (sin(angle) * innerR).toFloat()
                drawLine(
                    color = if (isMajor) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.25f),
                    start = androidx.compose.ui.geometry.Offset(x1, y1),
                    end = androidx.compose.ui.geometry.Offset(x2, y2),
                    strokeWidth = if (isMajor) 3.5f else 1.8f,
                    cap = StrokeCap.Round
                )
            }

            // progress sweep
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progressFraction,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        androidx.compose.material3.Text(
            text = centerLabel,
            color = Color.White,
            style = TextStyle(fontSize = 42.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Light)
        )
    }
}
