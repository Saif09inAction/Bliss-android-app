package com.laiza.worker.domain.models

data class Employee(
    val id: String,
    val name: String,
    val phone: String,
    val joiningDate: String,
    val monthlySalary: Double,
    val profilePhotoUrl: String?,
    val attendancePercentage: Double = 0.0,
    val role: Role = Role.STAFF,
    /** Overpaid kharcha carried forward from a previous order — auto-applied to the next bill. */
    val creditBalance: Double = 0.0,
    /** Old remaining owed to this kaariger when migrating onto the software. */
    val openingBalance: Double = 0.0,
    /** Legacy unpaid week kharcha not yet folded into opening (cleared on next Saturday bill). */
    val oldKharcha: Double = 0.0,
    /**
     * Optional per-staff shift. When blank, punch late/early uses company
     * Attendance settings defaults.
     */
    val dailySignInTime: String = "",
    val dailySignOutTime: String = "",
    val salaryRemaining: Double? = null
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
