package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.ActivityLog

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey
    val id: String,
    val userName: String,
    val action: String,
    val module: String,
    val date: String,
    val time: String
) {
    fun toDomain(): ActivityLog {
        return ActivityLog(
            id = id,
            userName = userName,
            action = action,
            module = module,
            date = date,
            time = time
        )
    }

    companion object {
        fun fromDomain(domain: ActivityLog): ActivityLogEntity {
            return ActivityLogEntity(
                id = domain.id,
                userName = domain.userName,
                action = domain.action,
                module = domain.module,
                date = domain.date,
                time = domain.time
            )
        }
    }
}
