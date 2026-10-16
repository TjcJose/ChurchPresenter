package org.churchpresenter.recording

/**
 * The elapsed clock, as a recording application shows it: `HH:MM:SS`.
 *
 * Kept a pure function so it can be driven directly; the tab only prints what it returns.
 */
fun formatRecordingElapsed(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
