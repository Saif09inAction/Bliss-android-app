package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.NotificationAlert

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val employeeId: String?,
    val title: String,
    val message: String,
    val date: String,
    val time: String,
    val isRead: Boolean = false
) {
    fun toDomain(): NotificationAlert {
        return NotificationAlert(
            id = id,
            employeeId = employeeId,
            title = title,
            message = message,
            date = date,
            time = time,
            isRead = isRead
        )
    }

    companion object {
        fun fromDomain(domain: NotificationAlert): NotificationEntity {
            return NotificationEntity(
                id = domain.id,
                employeeId = domain.employeeId,
                title = domain.title,
                message = domain.message,
                date = domain.date,
                time = domain.time,
                isRead = domain.isRead
            )
        }
    }
}
