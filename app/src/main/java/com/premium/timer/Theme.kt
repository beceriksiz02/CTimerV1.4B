package com.premium.timer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AccentPurple = Color(0xFF6C5CE7)
val AccentTeal = Color(0xFF00CEC9)
val AccentAmber = Color(0xFFFDCB6E)
val AccentRose = Color(0xFFFF6B81)
val SurfaceDark = Color(0xFF0B0B0F)
val SurfaceCard = Color(0xFF16161D)
val TextMuted = Color(0xFF8E8E9A)

val PremiumPalette = listOf(AccentPurple, AccentTeal, AccentAmber, AccentRose, Color(0xFF55E6C1), Color(0xFFEE5A6F))

private val AppColors = darkColorScheme(
    primary = AccentPurple,
    secondary = AccentTeal,
    background = Color.Black,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun PremiumTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
