package com.laiza.worker.core.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laiza.worker.core.local.entity.OfflineSyncRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncRecord(record: OfflineSyncRecord): Long

    @Query("SELECT * FROM offline_sync_records ORDER BY timestamp ASC")
    fun getAllSyncRecords(): Flow<List<OfflineSyncRecord>>

    @Delete
    suspend fun deleteSyncRecord(record: OfflineSyncRecord)

    @Query("DELETE FROM offline_sync_records")
    suspend fun clearAllRecords()
}
