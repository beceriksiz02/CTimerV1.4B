package com.premium.timer.timing

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Returns the current elapsedRealtime(), refreshed every [intervalMs].
 *
 * IMPORTANT: call this directly inside whatever composable actually displays the time
 * (e.g. inside TimerCard, inside the fullscreen content) rather than only at a parent and
 * passing an "unused" value down — Compose's skip optimization can otherwise decide a child
 * composable doesn't need to recompose because none of ITS declared parameters changed, even
 * if a sibling value ticked. Subscribing to the state read at the point of use guarantees the
 * composable actually re-executes on every tick.
 *
 * Pass a smaller [intervalMs] when displaying tenths/hundredths of a second — a 200ms tick
 * makes decimal digits visibly stutter. ~8ms (~120Hz) matches high refresh-rate displays;
 * lower-precision views (plain HH:MM:SS) don't need faster than ~200ms since the seconds
 * digit only changes once a second regardless of how often we check.
 *
 * This value is only ever used to trigger recomposition and to pass as the "now" argument into
 * TimeEngine's pure functions — it is never itself treated as the stored source of truth, and
 * a faster tick rate has zero effect on timing accuracy, only on how often the (always correct)
 * value is redrawn.
 */
@Composable
fun rememberTickingNow(intervalMs: Long = 200L): Long {
    var now by remember(intervalMs) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            delay(intervalMs)
            now = SystemClock.elapsedRealtime()
        }
    }
    return now
}

/** Picks an appropriate tick interval for a given display precision. */
fun tickIntervalFor(precision: DisplayPrecision): Long = when (precision) {
    DisplayPrecision.HOURS_MIN_SEC, DisplayPrecision.MIN_SEC -> 200L
    DisplayPrecision.MIN_SEC_TENTHS -> 33L   // ~30fps, plenty smooth for a single decimal digit
    DisplayPrecision.MIN_SEC_HUNDREDTHS -> 8L // ~120Hz, matches high refresh-rate displays
}
