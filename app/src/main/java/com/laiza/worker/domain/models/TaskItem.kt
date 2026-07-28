package com.laiza.worker.domain.models

data class TaskItem(
    val id: String,
    val name: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val deadline: String
)

enum class TaskPriority {
    HIGH,
    MEDIUM,
    LOW
}

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
