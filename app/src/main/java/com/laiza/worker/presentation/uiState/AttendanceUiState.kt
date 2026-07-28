package com.laiza.worker.presentation.uiState

import com.laiza.worker.domain.models.Attendance

sealed interface AttendanceHistoryUiState {
    object Loading : AttendanceHistoryUiState
    data class Success(val history: List<Attendance>) : AttendanceHistoryUiState
    data class Error(val message: String) : AttendanceHistoryUiState
}

data class TodayPunchState(
    val clockInTime: String? = null,
    val clockOutTime: String? = null,
    val workingHours: String? = null,
    val attendanceStatus: String = "Not Marked"
)

data class LocationDetails(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val address: String,
    val city: String,
    val state: String,
    val country: String,
    val timestamp: Long
)

sealed interface GPSState {
    object Idle : GPSState
    object Fetching : GPSState
    data class Success(val details: LocationDetails) : GPSState
    data class Error(val message: String) : GPSState
}

sealed interface PunchSubmitState {
    object Idle : PunchSubmitState
    object Submitting : PunchSubmitState
    object Success : PunchSubmitState
    data class Error(val message: String) : PunchSubmitState
}

sealed interface FaceDetectionState {
    object Idle : FaceDetectionState
    object Processing : FaceDetectionState
    data class Success(val filePath: String) : FaceDetectionState
    data class Failure(val message: String) : FaceDetectionState
}
