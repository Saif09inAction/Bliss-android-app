package com.laiza.worker.core.di

import android.content.Context
import androidx.room.Room
import com.laiza.worker.core.local.LaizaDatabase
import com.laiza.worker.core.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LaizaDatabase {
        return Room.databaseBuilder(
            context,
            LaizaDatabase::class.java,
            LaizaDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideOfflineSyncDao(database: LaizaDatabase): OfflineSyncDao {
        return database.offlineSyncDao()
    }

    @Provides
    @Singleton
    fun provideEmployeeDao(database: LaizaDatabase): EmployeeDao {
        return database.employeeDao()
    }

    @Provides
    @Singleton
    fun provideAttendanceDao(database: LaizaDatabase): AttendanceDao {
        return database.attendanceDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(database: LaizaDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    @Singleton
    fun provideInventoryDao(database: LaizaDatabase): InventoryDao {
        return database.inventoryDao()
    }

    @Provides
    @Singleton
    fun provideAuditLogDao(database: LaizaDatabase): AuditLogDao {
        return database.auditLogDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(database: LaizaDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    @Singleton
    fun provideSaleDao(database: LaizaDatabase): SaleDao {
        return database.saleDao()
    }
}

