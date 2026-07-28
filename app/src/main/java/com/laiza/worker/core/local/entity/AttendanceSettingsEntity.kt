package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.AttendanceSettings

@Entity(tableName = "attendance_settings")
data class AttendanceSettingsEntity(
    @PrimaryKey
    val id: String = "singleton",
    val dailySignInTime: String,
    val dailySignOutTime: String
) {
    fun toDomain(): AttendanceSettings {
        return AttendanceSettings(
            dailySignInTime = dailySignInTime,
            dailySignOutTime = dailySignOutTime
        )
    }

    companion object {
        fun fromDomain(domain: AttendanceSettings): AttendanceSettingsEntity {
            return AttendanceSettingsEntity(
                dailySignInTime = domain.dailySignInTime,
                dailySignOutTime = domain.dailySignOutTime
            )
        }
    }
}
