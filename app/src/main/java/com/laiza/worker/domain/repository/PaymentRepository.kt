package com.laiza.worker.domain.repository

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.PaymentTransaction
import com.laiza.worker.domain.models.PaymentType
import com.laiza.worker.domain.models.SalaryBalanceSheet
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getPaymentsForEmployee(employeeId: String): Flow<List<PaymentTransaction>>
    fun getAllTransactions(): Flow<List<PaymentTransaction>>
    fun getSalaryBalanceSheet(employeeId: String): Flow<SalaryBalanceSheet>
    fun addPayment(
        employeeId: String,
        amount: Double,
        type: PaymentType,
        remarks: String?,
        adminName: String
    ): Flow<Resource<Unit>>
}
