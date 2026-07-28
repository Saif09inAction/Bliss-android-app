package com.laiza.worker.domain.models

data class DashboardStats(
    val attendanceStatus: String,
    val pendingTasksCount: Int,
    val monthlySalary: String,
    val advanceRemaining: String
)
