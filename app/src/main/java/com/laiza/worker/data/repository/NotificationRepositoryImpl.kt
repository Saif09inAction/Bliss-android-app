package com.laiza.worker.data.repository

import com.laiza.worker.core.local.dao.NotificationDao
import com.laiza.worker.core.local.entity.NotificationEntity
import com.laiza.worker.domain.models.NotificationAlert
import com.laiza.worker.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getNotifications(employeeId: String?): Flow<List<NotificationAlert>> {
        return notificationDao.getNotificationsForEmployee(employeeId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addNotification(notification: NotificationAlert) {
        notificationDao.insertNotification(NotificationEntity.fromDomain(notification))
    }

    override suspend fun markAsRead(id: String) {
        notificationDao.markAsRead(id)
    }
}
