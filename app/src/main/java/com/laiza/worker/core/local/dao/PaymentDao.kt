package com.laiza.worker.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laiza.worker.core.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE employeeId = :employeeId ORDER BY date DESC, time DESC")
    fun getPaymentsForEmployee(employeeId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY date DESC, time DESC")
    fun getAllTransactions(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}
