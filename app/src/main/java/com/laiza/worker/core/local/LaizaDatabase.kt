package com.laiza.worker.core.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.laiza.worker.core.local.dao.*
import com.laiza.worker.core.local.entity.*

@Database(
    entities = [
        OfflineSyncRecord::class,
        EmployeeEntity::class,
        AttendanceEntity::class,
        AttendanceSettingsEntity::class,
        PaymentEntity::class,
        RawMaterialEntity::class,
        FinishedProductEntity::class,
        FinishedProductComponentEntity::class,
        ActivityLogEntity::class,
        NotificationEntity::class,
        SaleRecordEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class LaizaDatabase : RoomDatabase() {
    abstract fun offlineSyncDao(): OfflineSyncDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationDao(): NotificationDao
    abstract fun saleDao(): SaleDao

    companion object {
        const val DATABASE_NAME = "laiza_database"
    }
}
