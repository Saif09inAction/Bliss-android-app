package com.laiza.worker.domain.repository

import com.laiza.worker.domain.models.ActivityLog
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getLogs(): Flow<List<ActivityLog>>
    suspend fun addLog(userName: String, action: String, module: String)
}
