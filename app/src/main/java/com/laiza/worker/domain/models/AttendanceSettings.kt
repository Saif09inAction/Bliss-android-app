package com.laiza.worker.domain.models

data class AttendanceSettings(
    val dailySignInTime: String, // format "HH:mm" (e.g. "09:00")
    val dailySignOutTime: String  // format "HH:mm" (e.g. "18:00")
)
