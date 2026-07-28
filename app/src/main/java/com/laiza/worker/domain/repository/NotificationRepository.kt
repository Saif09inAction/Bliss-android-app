package com.laiza.worker.domain.repository

import com.laiza.worker.domain.models.NotificationAlert
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(employeeId: String?): Flow<List<NotificationAlert>>
    suspend fun addNotification(notification: NotificationAlert)
    suspend fun markAsRead(id: String)
}
