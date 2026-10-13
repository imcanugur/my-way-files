package tr.canugur.mywayfiles.core.util

import java.util.Locale

/**
 * Standard duration formatter converting milliseconds to human-readable time (mm:ss or hh:mm:ss).
 */
object DurationFormatter {

    fun format(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun format(durationMs: Int): String = format(durationMs.toLong())
}
