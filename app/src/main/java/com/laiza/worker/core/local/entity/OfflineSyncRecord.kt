package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_sync_records")
data class OfflineSyncRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val endpoint: String,
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
