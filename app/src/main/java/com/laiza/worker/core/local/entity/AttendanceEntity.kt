package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.Attendance
import com.laiza.worker.domain.models.AttendanceStatus

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey
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
    val status: String,
    val lateMinutes: Int = 0,
    val workingHours: Double = 0.0
) {
    fun toDomain(): Attendance {
        return Attendance(
            id = id,
            employeeId = employeeId,
            date = date,
            signInTime = signInTime,
            signOutTime = signOutTime,
            signInGps = signInGps,
            signOutGps = signOutGps,
            signInAddress = signInAddress,
            signOutAddress = signOutAddress,
            signInImageLocalPath = signInImageLocalPath,
            signOutImageLocalPath = signOutImageLocalPath,
            status = AttendanceStatus.valueOf(status),
            lateMinutes = lateMinutes,
            workingHours = workingHours
        )
    }

    companion object {
        fun fromDomain(domain: Attendance): AttendanceEntity {
            return AttendanceEntity(
                id = domain.id,
                employeeId = domain.employeeId,
                date = domain.date,
                signInTime = domain.signInTime,
                signOutTime = domain.signOutTime,
                signInGps = domain.signInGps,
                signOutGps = domain.signOutGps,
                signInAddress = domain.signInAddress,
                signOutAddress = domain.signOutAddress,
                signInImageLocalPath = domain.signInImageLocalPath,
                signOutImageLocalPath = domain.signOutImageLocalPath,
                status = domain.status.name,
                lateMinutes = domain.lateMinutes,
                workingHours = domain.workingHours
            )
        }
    }
}
