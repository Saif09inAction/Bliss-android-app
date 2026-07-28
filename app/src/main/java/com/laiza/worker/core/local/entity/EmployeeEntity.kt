package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.Employee
import com.laiza.worker.domain.models.Role

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val joiningDate: String,
    val monthlySalary: Double,
    val profilePhotoUrl: String?,
    val attendancePercentage: Double = 0.0,
    val role: String = Role.STAFF.name
) {
    fun toDomain(): Employee {
        return Employee(
            id = id,
            name = name,
            phone = phone,
            joiningDate = joiningDate,
            monthlySalary = monthlySalary,
            profilePhotoUrl = profilePhotoUrl,
            attendancePercentage = attendancePercentage,
            role = Role.fromFirestore(role)
        )
    }

    companion object {
        fun fromDomain(domain: Employee): EmployeeEntity {
            return EmployeeEntity(
                id = domain.id,
                name = domain.name,
                phone = domain.phone,
                joiningDate = domain.joiningDate,
                monthlySalary = domain.monthlySalary,
                profilePhotoUrl = domain.profilePhotoUrl,
                attendancePercentage = domain.attendancePercentage,
                role = domain.role.name
            )
        }
    }
}
