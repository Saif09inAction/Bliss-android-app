package com.laiza.worker.domain.usecases

import com.laiza.worker.core.utils.DateFormatter
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.Attendance
import com.laiza.worker.domain.models.AttendanceSettings
import com.laiza.worker.domain.models.AttendanceStatus
import com.laiza.worker.domain.models.AttendanceType
import com.laiza.worker.domain.models.Employee
import com.laiza.worker.domain.repository.AttendanceRepository
import com.laiza.worker.domain.repository.EmployeeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class MarkAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository
) {
    operator fun invoke(
        employeeId: String,
        punchType: AttendanceType,
        gps: String?,
        address: String?,
        imagePath: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeStr = DateFormatter.nowTime12HourWithSeconds()
            // Stable doc id so sign-in/out update the same Firestore document
            val recordId = "${employeeId}_$dateStr"

            val globalSettings = attendanceRepository.getFreshSettings()
            val employee = employeeRepository.getEmployee(employeeId).first()
            val settings = resolveShiftSettings(employee, globalSettings)
            val todayHistory = attendanceRepository.getEmployeeAttendanceHistory(employeeId).first()
            val existingToday = todayHistory.firstOrNull { it.date == dateStr }
                ?: todayHistory.firstOrNull { it.id == recordId }

            val currentLocalTime = safeParseTime(timeStr, LocalTime.of(9, 0))
            val expectedSignIn = safeParseTime(settings.dailySignInTime, LocalTime.of(9, 0))
            val expectedSignOut = safeParseTime(settings.dailySignOutTime, LocalTime.of(18, 0))

            val attendance = if (punchType == AttendanceType.SIGN_IN) {
                val lateMins = if (currentLocalTime.isAfter(expectedSignIn)) {
                    ChronoUnit.MINUTES.between(expectedSignIn, currentLocalTime).toInt()
                } else {
                    0
                }

                val status = if (lateMins > 0) AttendanceStatus.LATE else AttendanceStatus.ON_TIME

                Attendance(
                    id = existingToday?.id ?: recordId,
                    employeeId = employeeId,
                    date = dateStr,
                    signInTime = timeStr,
                    signOutTime = existingToday?.signOutTime,
                    signInGps = gps,
                    signOutGps = existingToday?.signOutGps,
                    signInAddress = address,
                    signOutAddress = existingToday?.signOutAddress,
                    signInImageLocalPath = imagePath,
                    signOutImageLocalPath = existingToday?.signOutImageLocalPath,
                    status = status,
                    lateMinutes = lateMins,
                    workingHours = existingToday?.workingHours ?: 0.0
                )
            } else {
                if (existingToday == null) {
                    emit(Resource.Error("You must check in before checking out"))
                    return@flow
                }

                val signInTimeLocal = safeParseTime(existingToday.signInTime, currentLocalTime)
                val workingHrs = computeShiftWorkingHours(
                    signInTimeLocal,
                    currentLocalTime,
                    expectedSignIn,
                    expectedSignOut
                )

                val leftEarly = currentLocalTime.isBefore(expectedSignOut)
                val status = when {
                    leftEarly -> AttendanceStatus.LEFT_EARLY
                    existingToday.status == AttendanceStatus.LATE -> AttendanceStatus.LATE
                    else -> AttendanceStatus.PRESENT
                }

                existingToday.copy(
                    id = if (existingToday.id.isBlank()) recordId else existingToday.id,
                    signOutTime = timeStr,
                    signOutGps = gps,
                    signOutAddress = address,
                    signOutImageLocalPath = imagePath,
                    status = status,
                    workingHours = String.format(Locale.US, "%.2f", workingHrs).toDouble()
                )
            }

            val result = attendanceRepository
                .saveAttendance(attendance)
                .filter { it !is Resource.Loading }
                .first()

            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record attendance"))
        }
    }

    /** Staff custom shift if set; otherwise company Attendance defaults. */
    private fun resolveShiftSettings(
        employee: Employee?,
        global: AttendanceSettings
    ): AttendanceSettings {
        val inTime = employee?.dailySignInTime?.trim().orEmpty()
        val outTime = employee?.dailySignOutTime?.trim().orEmpty()
        if (inTime.isEmpty() && outTime.isEmpty()) return global
        return AttendanceSettings(
            dailySignInTime = inTime.ifEmpty { global.dailySignInTime },
            dailySignOutTime = outTime.ifEmpty { global.dailySignOutTime }
        )
    }

    /** Paid hours: only within scheduled shift; late clock-out does not add extra. */
    private fun computeShiftWorkingHours(
        signIn: LocalTime,
        signOut: LocalTime,
        shiftIn: LocalTime,
        shiftOut: LocalTime
    ): Double {
        val effectiveStart = if (signIn.isBefore(shiftIn)) shiftIn else signIn
        val effectiveEnd = if (signOut.isAfter(shiftOut)) shiftOut else signOut
        if (!effectiveEnd.isAfter(effectiveStart)) return 0.0
        val workedMins = ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd)
        var shiftMins = ChronoUnit.MINUTES.between(shiftIn, shiftOut)
        if (shiftMins <= 0) shiftMins += 24 * 60
        val creditedMins = minOf(workedMins, shiftMins)
        return String.format(Locale.US, "%.2f", creditedMins / 60.0).toDouble()
    }

    private fun safeParseTime(timeStr: String?, defaultTime: LocalTime): LocalTime {
        if (timeStr.isNullOrBlank()) return defaultTime
        val formats = listOf(
            "h:mm:ss a",
            "hh:mm:ss a",
            "HH:mm:ss",
            "HH:mm",
            "h:mm a",
            "hh:mm a",
            "H:mm"
        )
        for (fmt in formats) {
            try {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(fmt, Locale.US))
            } catch (_: Exception) {
            }
        }
        return defaultTime
    }
}
