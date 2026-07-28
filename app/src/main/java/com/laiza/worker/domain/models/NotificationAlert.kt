package com.laiza.worker.domain.models

data class NotificationAlert(
    val id: String,
    val employeeId: String?,
    val title: String,
    val message: String,
    val date: String,
    val time: String,
    val isRead: Boolean = false
)
