package tr.canugur.mywayfiles.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe utility for formatting file modification timestamps.
 */
object DateTimeFormatter {

    fun format(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }
}
