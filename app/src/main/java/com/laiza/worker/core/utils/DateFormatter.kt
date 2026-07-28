package com.laiza.worker.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {

    private val apiDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val databaseDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

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
}
