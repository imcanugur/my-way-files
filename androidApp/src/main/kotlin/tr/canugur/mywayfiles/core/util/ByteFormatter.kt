package tr.canugur.mywayfiles.core.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * Thread-safe, pure utility for human-readable byte calculations.
 */
object ByteFormatter {

    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    fun format(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, UNITS.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.getDefault(), "%.1f %s", value, UNITS[digitGroups])
    }
}
