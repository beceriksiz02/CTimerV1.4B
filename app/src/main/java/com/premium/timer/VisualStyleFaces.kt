package com.premium.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Thick filled disk/pie that depletes as progressFraction falls (or fills as it rises). */
@Composable
fun ThickDiskFace(progressFraction: Float, accent: Color, centerLabel: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f * 0.92f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = Color.White.copy(alpha = 0.06f), radius = radius, center = center)

            drawArc(
                color = accent.copy(alpha = 0.85f),
                startAngle = -90f,
                sweepAngle = 360f * progressFraction.coerceIn(0f, 1f),
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // subtle rim so the disk doesn't look flat against a busy background
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = radius,
                center = center,
                style = Stroke(width = size.minDimension * 0.01f)
            )
        }
        androidx.compose.material3.Text(
            text = centerLabel,
            color = Color.White,
            style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

/** A ring built from discrete segments that light up one by one — visually distinct from a
 *  smooth sweep, useful for people who find continuous motion distracting. */
@Composable
fun SegmentedRingFace(
    progressFraction: Float,
    accent: Color,
    centerLabel: String,
    modifier: Modifier = Modifier,
    segmentCount: Int = 40
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.06f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val litCount = (segmentCount * progressFraction.coerceIn(0f, 1f)).toInt()
            val gapDegrees = 2.5f
            val segmentSweep = (360f / segmentCount) - gapDegrees

            for (i in 0 until segmentCount) {
                val startAngle = -90f + i * (360f / segmentCount) + gapDegrees / 2f
                drawArc(
                    color = if (i < litCount) accent else Color.White.copy(alpha = 0.10f),
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
        androidx.compose.material3.Text(
            text = centerLabel,
            color = Color.White,
            style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Light)
        )
    }
}
