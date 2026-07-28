package com.laiza.worker.domain.models

data class Employee(
    val id: String,
    val name: String,
    val phone: String,
    val joiningDate: String,
    val monthlySalary: Double,
    val profilePhotoUrl: String?,
    val attendancePercentage: Double = 0.0,
    val role: Role = Role.STAFF
)

data class EmployeeExtraProfile(
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val designation: String = "",
    val employeeId: String = "",
    val accountHolder: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val branch: String = "",
    val upiId: String = ""
)
