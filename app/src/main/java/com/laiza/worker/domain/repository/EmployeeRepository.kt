package com.laiza.worker.domain.repository

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.Employee
import kotlinx.coroutines.flow.Flow

interface EmployeeRepository {
    fun getEmployee(id: String): Flow<Employee?>
    fun getAllEmployees(): Flow<List<Employee>>
    fun addEmployee(employee: Employee): Flow<Resource<Unit>>
    fun updateEmployee(employee: Employee): Flow<Resource<Unit>>
    fun deleteEmployee(id: String): Flow<Resource<Unit>>
    fun searchEmployees(query: String): Flow<List<Employee>>
}
