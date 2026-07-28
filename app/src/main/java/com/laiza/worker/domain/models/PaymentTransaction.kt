package com.laiza.worker.domain.models

data class PaymentTransaction(
    val id: String,
    val employeeId: String,
    val amount: Double,
    val type: PaymentType,
    val date: String,
    val time: String,
    val remarks: String?,
    val createdBy: String
)

data class SalaryBalanceSheet(
    val employeeId: String,
    val monthlySalary: Double,
    val salaryReceived: Double,
    val salaryRemaining: Double,
    val advanceTaken: Double,
    val extraPayments: Double,
    val pendingSalary: Double
)

enum class PaymentType {
    SALARY_PAYMENT, ADVANCE, EXTRA_PAYMENT, DEDUCTION
}
