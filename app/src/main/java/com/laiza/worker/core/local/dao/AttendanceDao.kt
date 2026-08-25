package com.laiza.worker.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laiza.worker.core.local.entity.AttendanceEntity
import com.laiza.worker.core.local.entity.AttendanceSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE id = :id")
    fun getAttendanceById(id: String): Flow<AttendanceEntity?>

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId ORDER BY date DESC, signInTime DESC")
    fun getEmployeeAttendanceHistory(employeeId: String): Flow<List<AttendanceEntity>>

    @Query("DELETE FROM attendance WHERE employeeId = :employeeId")
    suspend fun deleteAttendanceForEmployee(employeeId: String)

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getTodayAttendance(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date < :cutoffDate AND (signInImageLocalPath IS NOT NULL OR signOutImageLocalPath IS NOT NULL)")
    suspend fun getRecordsWithOldImages(cutoffDate: String): List<AttendanceEntity>

    @Query("UPDATE attendance SET signInImageLocalPath = NULL, signOutImageLocalPath = NULL WHERE date < :cutoffDate")
    suspend fun clearOldImagePaths(cutoffDate: String): Int

    // Settings Queries (Singleton logic)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AttendanceSettingsEntity)

    @Query("SELECT * FROM attendance_settings WHERE id = 'singleton'")
    fun getSettings(): Flow<AttendanceSettingsEntity?>
}
