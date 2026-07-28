package com.laiza.worker.domain.usecases

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.Attendance
import com.laiza.worker.domain.models.AttendanceStatus
import com.laiza.worker.domain.models.AttendanceType
import com.laiza.worker.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class MarkAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
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
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val settings = attendanceRepository.getSettings().first()
            val todayHistory = attendanceRepository.getEmployeeAttendanceHistory(employeeId).first()
            val existingToday = todayHistory.firstOrNull { it.date == dateStr }

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
                    id = UUID.randomUUID().toString(),
                    employeeId = employeeId,
                    date = dateStr,
                    signInTime = timeStr,
                    signOutTime = null,
                    signInGps = gps,
                    signOutGps = null,
                    signInAddress = address,
                    signOutAddress = null,
                    signInImageLocalPath = imagePath,
                    signOutImageLocalPath = null,
                    status = status,
                    lateMinutes = lateMins,
                    workingHours = 0.0
                )
            } else {
                if (existingToday == null) {
                    emit(Resource.Error("You must check in before checking out"))
                    return@flow
                }

                val signInTimeLocal = safeParseTime(existingToday.signInTime, currentLocalTime)
                val workingHrs = ChronoUnit.MINUTES.between(signInTimeLocal, currentLocalTime).toDouble() / 60.0

                val leftEarly = currentLocalTime.isBefore(expectedSignOut)
                val status = when {
                    leftEarly -> AttendanceStatus.LEFT_EARLY
                    existingToday.status == AttendanceStatus.LATE -> AttendanceStatus.LATE
                    else -> AttendanceStatus.PRESENT
                }

                existingToday.copy(
                    signOutTime = timeStr,
                    signOutGps = gps,
                    signOutAddress = address,
                    signOutImageLocalPath = imagePath,
                    status = status,
                    workingHours = String.format(Locale.US, "%.2f", workingHrs).toDouble()
                )
            }

            attendanceRepository.saveAttendance(attendance).collect { resource ->
                emit(resource)
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record attendance"))
        }
    }

    private fun safeParseTime(timeStr: String?, defaultTime: LocalTime): LocalTime {
        if (timeStr.isNullOrBlank()) return defaultTime
        val formats = listOf("HH:mm:ss", "HH:mm", "h:mm a", "hh:mm a", "H:mm")
        for (fmt in formats) {
            try {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(fmt, Locale.US))
            } catch (e: Exception) {}
        }
        return defaultTime
    }
}
