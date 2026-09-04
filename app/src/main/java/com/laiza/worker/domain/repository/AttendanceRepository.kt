package com.laiza.worker.domain.repository

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.Attendance
import com.laiza.worker.domain.models.AttendanceSettings
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun getAttendanceRecord(id: String): Flow<Attendance?>
    fun getEmployeeAttendanceHistory(employeeId: String): Flow<List<Attendance>>
    fun getTodayAttendance(): Flow<List<Attendance>>
    fun saveAttendance(attendance: Attendance): Flow<Resource<Unit>>
    fun getSettings(): Flow<AttendanceSettings>
    /** Prefer live Firestore settings for punch calculations. */
    suspend fun getFreshSettings(): AttendanceSettings
    fun saveSettings(settings: AttendanceSettings): Flow<Resource<Unit>>
}
