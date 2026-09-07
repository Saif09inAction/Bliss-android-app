package com.laiza.worker.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Join-anniversary pay/attendance month.
 * Join 3 Jan → period runs 3 Jan – 2 Feb, then 3 Feb – 2 Mar, etc.
 */
data class JoinMonthPeriod(
    val start: String,
    val end: String,
    val daysInPeriod: Int
)

/** Firestore calendar_days/{yyyy-MM-dd} override. */
data class CalendarDayOverride(
    val date: String,
    val kind: String, // HOLIDAY | WORKING
    val appliesTo: String = "ALL", // ALL | SELECTED
    val employeeIds: List<String> = emptyList()
)

object PayPeriodUtils {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayIso(): String = sdf.format(Date())

    /**
     * Current join-month window that contains [asOfDate].
     * Falls back to calendar month when joining date is missing/invalid.
     */
    fun currentJoinMonthPeriod(joiningDateStr: String?, asOfDate: String = todayIso()): JoinMonthPeriod {
        val joinDate = joiningDateStr?.trim()?.takeIf { it.isNotEmpty() }?.let {
            try {
                sdf.parse(it)
            } catch (_: Exception) {
                null
            }
        }
        val todayDate = try {
            sdf.parse(asOfDate)
        } catch (_: Exception) {
            Date()
        } ?: Date()

        if (joinDate == null) {
            val cal = Calendar.getInstance().apply {
                time = todayDate
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val start = sdf.format(cal.time)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            val end = sdf.format(cal.time)
            return JoinMonthPeriod(start, end, maxDay)
        }

        val calJoin = Calendar.getInstance().apply { time = joinDate }
        val joinDay = calJoin.get(Calendar.DAY_OF_MONTH)

        val calToday = Calendar.getInstance().apply { time = todayDate }
        val todayYear = calToday.get(Calendar.YEAR)
        val todayMonth = calToday.get(Calendar.MONTH)
        val todayDay = calToday.get(Calendar.DAY_OF_MONTH)

        val calStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, todayYear)
            set(Calendar.MONTH, todayMonth)
            val maxDaysThisMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(joinDay, maxDaysThisMonth))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Before this month's anniversary day → still in previous join-month.
        if (todayDay < joinDay) {
            calStart.add(Calendar.MONTH, -1)
            val maxDaysPrevMonth = calStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            calStart.set(Calendar.DAY_OF_MONTH, minOf(joinDay, maxDaysPrevMonth))
        }

        // Never start before actual joining date.
        if (calStart.time.before(joinDate)) {
            calStart.time = joinDate
        }

        val calEnd = (calStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }

        val startStr = sdf.format(calStart.time)
        val endStr = sdf.format(calEnd.time)
        val diffMs = calEnd.timeInMillis - calStart.timeInMillis
        val daysInPeriod = (diffMs / (1000 * 60 * 60 * 24)).toInt() + 1

        return JoinMonthPeriod(startStr, endStr, maxOf(1, daysInPeriod))
    }

    fun isDateInPeriod(date: String, period: JoinMonthPeriod): Boolean {
        if (date.isBlank()) return false
        return date >= period.start && date <= period.end
    }

    fun eachIsoDateInclusive(start: String, end: String): List<String> {
        if (start.isBlank() || end.isBlank() || end < start) return emptyList()
        val out = mutableListOf<String>()
        val cal = Calendar.getInstance().apply {
            time = sdf.parse(start) ?: return emptyList()
        }
        val endTime = sdf.parse(end)?.time ?: return emptyList()
        while (cal.timeInMillis <= endTime) {
            out += sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return out
    }

    fun isSunday(dateStr: String): Boolean {
        val date = try {
            sdf.parse(dateStr)
        } catch (_: Exception) {
            null
        } ?: return false
        val cal = Calendar.getInstance().apply { time = date }
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    /** Default: Sunday = holiday, else working. */
    fun defaultDayKind(dateStr: String): String =
        if (isSunday(dateStr)) "HOLIDAY" else "WORKING"

    /**
     * Resolve HOLIDAY vs WORKING for salary.
     * Matches admin-web: Sunday/admin holiday = paid full day (no punch needed).
     */
    fun resolveDayKind(
        dateStr: String,
        overrides: Map<String, CalendarDayOverride>,
        employeePhone: String? = null
    ): String {
        val override = overrides[dateStr] ?: return defaultDayKind(dateStr)
        if (override.appliesTo != "SELECTED" || employeePhone.isNullOrBlank()) {
            return override.kind
        }
        val listed = override.employeeIds.any {
            it.equals(employeePhone, ignoreCase = true)
        }
        return if (listed) override.kind else defaultDayKind(dateStr)
    }

    /** Sunday / admin holiday — paid as full present (salary not deducted). */
    fun isPaidOffDay(
        dateStr: String,
        overrides: Map<String, CalendarDayOverride>,
        employeePhone: String? = null
    ): Boolean = resolveDayKind(dateStr, overrides, employeePhone) == "HOLIDAY"
}
