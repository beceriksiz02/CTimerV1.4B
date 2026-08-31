package com.premium.timer.timing

/**
 * 5 substantially different display styles (Phase 3), not just color variants of one ring.
 */
enum class VisualStyle {
    DIGITAL,        // large plain digits, existing style
    ANALOG,         // round clock face with tick indicators + sweep, existing style
    MINIMAL,        // ultra-minimal: just the number, no chrome, no controls border
    THICK_DISK,     // filled pie/disk that empties or fills as time passes
    SEGMENTED_RING  // ring made of discrete segments that light up one by one
}

/**
 * Display precision — affects ONLY formatting, never the underlying timing accuracy.
 * TimeEngine always computes exact millisecond values; this enum just controls how many
 * of those digits get shown to the user.
 */
enum class DisplayPrecision(val label: String) {
    HOURS_MIN_SEC("HH:MM:SS"),
    MIN_SEC("MM:SS"),
    MIN_SEC_TENTHS("MM:SS.t"),
    MIN_SEC_HUNDREDTHS("MM:SS.tt")
}

fun formatWithPrecision(millis: Long, precision: DisplayPrecision): String {
    val totalMs = millis.coerceAtLeast(0)
    val h = totalMs / 3_600_000
    val m = (totalMs % 3_600_000) / 60_000
    val s = (totalMs % 60_000) / 1000
    val tenths = (totalMs % 1000) / 100
    val hundredths = (totalMs % 1000) / 10

    return when (precision) {
        DisplayPrecision.HOURS_MIN_SEC ->
            if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        DisplayPrecision.MIN_SEC -> {
            val totalMinutes = totalMs / 60_000
            "%02d:%02d".format(totalMinutes, s)
        }
        DisplayPrecision.MIN_SEC_TENTHS -> {
            val totalMinutes = totalMs / 60_000
            "%02d:%02d.%01d".format(totalMinutes, s, tenths)
        }
        DisplayPrecision.MIN_SEC_HUNDREDTHS -> {
            val totalMinutes = totalMs / 60_000
            "%02d:%02d.%02d".format(totalMinutes, s, hundredths)
        }
    }
}
