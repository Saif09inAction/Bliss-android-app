package com.laiza.worker.domain.models

data class ActivityLog(
    val id: String,
    val userName: String,
    val action: String,
    val module: String,
    val date: String,
    val time: String
)
