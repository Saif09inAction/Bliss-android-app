package com.laiza.worker.domain.models

data class Attendance(
    val id: String,
    val employeeId: String,
    val date: String,
    val signInTime: String?,
    val signOutTime: String?,
    val signInGps: String?,
    val signOutGps: String?,
    val signInAddress: String?,
    val signOutAddress: String?,
    val signInImageLocalPath: String?,
    val signOutImageLocalPath: String?,
    val status: AttendanceStatus,
    val lateMinutes: Int = 0,
    val workingHours: Double = 0.0
)

enum class AttendanceStatus {
    PRESENT, LATE, LEFT_EARLY, ON_TIME, ABSENT, HALF_DAY, FULL_DAY
}

/** Parse Firestore/Room status without crashing on unknown values. */
fun parseAttendanceStatus(raw: String?): AttendanceStatus {
    val value = raw?.trim()?.uppercase().orEmpty()
    if (value.isEmpty()) return AttendanceStatus.PRESENT
    return try {
        AttendanceStatus.valueOf(value)
    } catch (_: IllegalArgumentException) {
        when (value) {
            "HALF", "HALFDAY" -> AttendanceStatus.HALF_DAY
            "FULL", "FULLDAY" -> AttendanceStatus.FULL_DAY
            else -> AttendanceStatus.PRESENT
        }
    }
}

enum class AttendanceType {
    SIGN_IN, SIGN_OUT
}
