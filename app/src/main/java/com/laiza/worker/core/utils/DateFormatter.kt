package com.laiza.worker.core.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateFormatter {

    private val apiDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.US)
    private val databaseDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val storeTimePatterns = listOf(
        "h:mm:ss a",
        "hh:mm:ss a",
        "h:mm a",
        "hh:mm a",
        "HH:mm:ss",
        "HH:mm",
        "H:mm"
    )

    /** Current clock time for new records (12-hour, e.g. "2:41 PM"). */
    fun nowTime12Hour(): String =
        SimpleDateFormat("h:mm a", Locale.US).format(Date())

    /** Current clock time with seconds for attendance/logs (e.g. "2:41:05 PM"). */
    fun nowTime12HourWithSeconds(): String =
        SimpleDateFormat("h:mm:ss a", Locale.US).format(Date())

    /** Convert any stored time string to 12-hour display. */
    fun formatStoredTime(time: String?): String {
        val raw = time?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        parseLocalTime(raw)?.let { return it.format(displayTimeFormatter) }
        return raw
    }

    /** Sort key HH:mm so mixed 12h/24h stored times compare correctly. */
    fun timeSortKey(time: String?): String {
        val local = parseLocalTime(time?.trim().orEmpty()) ?: return "00:00"
        return "%02d:%02d".format(local.hour, local.minute)
    }

    fun formatStoredDateTime(date: String?, time: String?): String {
        val day = date?.trim().orEmpty()
        val clock = formatStoredTime(time)
        return when {
            day.isEmpty() -> clock
            clock.isEmpty() -> day
            else -> "$day · $clock"
        }
    }

    fun formatEpochToDisplayDate(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.format(displayDateFormatter)
    }

    fun formatEpochToDisplayTime(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return localTime.format(displayTimeFormatter)
    }

    fun formatEpochToDisplayDateTime(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        return localDateTime.format(displayDateTimeFormatter)
    }

    fun formatToDatabaseDate(localDate: LocalDate): String {
        return localDate.format(databaseDateFormatter)
    }

    fun parseDatabaseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr, databaseDateFormatter)
        } catch (e: Exception) {
            null
        }
    }

    fun formatIsoStringToDisplay(isoStr: String): String {
        return try {
            val localDateTime = LocalDateTime.parse(isoStr, apiDateFormatter)
            localDateTime.format(displayDateTimeFormatter)
        } catch (e: Exception) {
            isoStr
        }
    }

    private fun parseLocalTime(timeStr: String): LocalTime? {
        for (pattern in storeTimePatterns) {
            try {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(pattern, Locale.US))
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }
}
