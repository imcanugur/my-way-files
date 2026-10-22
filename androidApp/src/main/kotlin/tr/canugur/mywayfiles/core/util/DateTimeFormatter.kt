package tr.canugur.mywayfiles.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Thread-safe utility for formatting file modification timestamps.
 */
object DateTimeFormatter {

    private data class FormatterState(
        val locale: Locale,
        val timeZoneId: String,
        val formatter: SimpleDateFormat
    )

    // A formatter is mutable and not thread-safe. Keep one per thread and refresh it if
    // Android's locale or time zone changes, avoiding construction for every visible row.
    private val formatterState = ThreadLocal<FormatterState>()

    fun format(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        val locale = Locale.getDefault()
        val timeZone = TimeZone.getDefault()
        val current = formatterState.get()
        val state = if (current == null || current.locale != locale || current.timeZoneId != timeZone.id) {
            FormatterState(
                locale = locale,
                timeZoneId = timeZone.id,
                formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", locale).apply {
                    this.timeZone = timeZone
                }
            ).also(formatterState::set)
        } else {
            current
        }
        return state.formatter.format(Date(epochMillis))
    }
}
