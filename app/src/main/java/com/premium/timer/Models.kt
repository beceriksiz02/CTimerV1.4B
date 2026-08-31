package com.premium.timer

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.premium.timer.timing.DisplayPrecision
import com.premium.timer.timing.TimerFontChoice
import com.premium.timer.timing.VisualStyle

/** How the running/idle time is rendered on the fullscreen view. */
enum class ClockStyle { DIGITAL, ANALOG }

/** Where the background comes from: a flat color (pitch black by default) or a custom photo. */
enum class BackgroundType { COLOR, IMAGE }

/**
 * Per-timer (and globally, for the stopwatch) appearance settings.
 * Kept separate from timing state so switching themes never touches the clock logic.
 *
 * The actual timing truth lives in TimerStateEntity/TimeEngine (see the `timing` and `data`
 * packages) — this class is purely visual. As of Phase 2/3, every field here is mirrored back
 * to the database (see MainActivity's persistence side-effect), so it survives app restarts.
 */
class BackgroundSettings(
    initialColor: Color = Color.Black,
    initialImageUri: Uri? = null,
    initialType: BackgroundType = BackgroundType.COLOR,
    initialStyle: ClockStyle = ClockStyle.DIGITAL,
    initialAccent: Color = Color(0xFF6C5CE7),
    initialVisualStyle: VisualStyle = VisualStyle.DIGITAL,
    initialPrecision: DisplayPrecision = DisplayPrecision.HOURS_MIN_SEC,
    initialFontChoice: TimerFontChoice = TimerFontChoice.MODERN
) {
    var type by mutableStateOf(initialType)
    var color by mutableStateOf(initialColor)
    var imageUri by mutableStateOf(initialImageUri)
    var clockStyle by mutableStateOf(initialStyle)
    var accent by mutableStateOf(initialAccent)
    var visualStyle by mutableStateOf(initialVisualStyle)
    var precision by mutableStateOf(initialPrecision)
    var fontChoice by mutableStateOf(initialFontChoice)
}
