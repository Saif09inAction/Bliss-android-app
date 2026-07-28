package com.laiza.worker.domain.models

data class RecentActivity(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val type: ActivityType
)

enum class ActivityType {
    CLOCK_IN,
    MATERIAL_ASSIGNED,
    SALARY_UPDATED,
    WORK_ASSIGNED
}
