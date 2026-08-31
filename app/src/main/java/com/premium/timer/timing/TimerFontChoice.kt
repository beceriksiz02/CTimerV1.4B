package com.premium.timer.timing

import androidx.compose.ui.text.font.FontFamily

/**
 * Deliberately built on Android's generic font families (Default/SansSerif/Serif/Monospace/
 * Cursive) rather than bundled custom font files — this guarantees every choice always renders
 * correctly with zero risk of a missing-resource crash, while still giving genuinely different
 * typographic character (geometric sans, monospace/digital-style, serif/editorial, rounded/soft).
 */
enum class TimerFontChoice(val label: String, val family: FontFamily) {
    MODERN("Modern", FontFamily.Default),
    GEOMETRIC("Geometric", FontFamily.SansSerif),
    DIGITAL("Digital mono", FontFamily.Monospace),
    EDITORIAL("Editorial serif", FontFamily.Serif),
    ROUNDED("Rounded", FontFamily.Cursive)
}
