package com.laiza.worker.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.core.utils.FirebaseStorageHelper
import com.laiza.worker.core.local.dao.EmployeeDao
import com.laiza.worker.core.local.entity.EmployeeEntity
import com.laiza.worker.domain.models.Employee
import com.laiza.worker.domain.repository.EmployeeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EmployeeRepositoryImpl @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val firestore: FirebaseFirestore,
    private val storageHelper: FirebaseStorageHelper,
    @ApplicationContext private val context: Context
) : EmployeeRepository {

    override fun getEmployee(id: String): Flow<Employee?> {
        return employeeDao.getEmployeeById(id).map { it?.toDomain() }
    }

    override fun getAllEmployees(): Flow<List<Employee>> {
        // Trigger background fetch from Firestore to refresh local Room DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchEmployeesFromFirestore()
                for (emp in list) {
                    employeeDao.insertEmployee(EmployeeEntity.fromDomain(emp))
                }
            } catch (e: Exception) {
                // Ignore sync fail
            }
        }
        return employeeDao.getAllEmployees().map { list -> list.map { it.toDomain() } }
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        firestore.collection("employees").addSnapshotListener { snapshots, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            if (snapshots != null) {
                repositoryScope.launch {
                    for (docChange in snapshots.documentChanges) {
                        val doc = docChange.document
                        val phone = doc.getString("phone") ?: doc.id
                        if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            try {
                                employeeDao.deleteEmployeeById(phone)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            val emp = Employee(
                                id = doc.getString("id") ?: doc.id,
                                name = doc.getString("name") ?: "",
                                phone = phone,
                                joiningDate = doc.getString("joiningDate") ?: "",
                                monthlySalary = doc.getDouble("monthlySalary") ?: 0.0,
                                profilePhotoUrl = doc.getString("profilePhotoUrl"),
                                attendancePercentage = doc.getDouble("attendancePercentage") ?: 0.0,
                                role = com.laiza.worker.domain.models.Role.fromFirestore(doc.getString("role")),
                                creditBalance = doc.getDouble("creditBalance") ?: 0.0
                            )
                            try {
                                employeeDao.insertEmployee(EmployeeEntity.fromDomain(emp))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun addEmployee(employee: Employee): Flow<Resource<Unit>> = flow {
        try {
            employeeDao.insertEmployee(EmployeeEntity.fromDomain(employee))
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save locally"))
            return@flow
        }

        repositoryScope.launch {
            try {
                var finalEmployee = employee
                val localPath = employee.profilePhotoUrl
                if (!localPath.isNullOrBlank() && !localPath.startsWith("http")) {
                    val downloadUrl = storageHelper.uploadImage(context, localPath, "employee_profiles")
                    if (downloadUrl != null) {
                        finalEmployee = employee.copy(profilePhotoUrl = downloadUrl)
                        employeeDao.insertEmployee(EmployeeEntity.fromDomain(finalEmployee))
                    }
                }
                saveEmployeeToFirestore(finalEmployee)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun updateEmployee(employee: Employee): Flow<Resource<Unit>> = flow {
        try {
            employeeDao.updateEmployee(EmployeeEntity.fromDomain(employee))
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update locally"))
            return@flow
        }

        repositoryScope.launch {
            try {
                var finalEmployee = employee
                val localPath = employee.profilePhotoUrl
                if (!localPath.isNullOrBlank() && !localPath.startsWith("http")) {
                    val downloadUrl = storageHelper.uploadImage(context, localPath, "employee_profiles")
                    if (downloadUrl != null) {
                        finalEmployee = employee.copy(profilePhotoUrl = downloadUrl)
                        employeeDao.updateEmployee(EmployeeEntity.fromDomain(finalEmployee))
                    }
                }
                saveEmployeeToFirestore(finalEmployee)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun deleteEmployee(id: String): Flow<Resource<Unit>> = flow {
        try {
            employeeDao.deleteEmployeeById(id)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete locally"))
            return@flow
        }

        repositoryScope.launch {
            try {
                deleteEmployeeFromFirestore(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun searchEmployees(query: String): Flow<List<Employee>> {
        val searchQuery = "%$query%"
        return employeeDao.searchEmployees(searchQuery).map { list -> list.map { it.toDomain() } }
    }

    private suspend fun fetchEmployeesFromFirestore(): List<Employee> = suspendCancellableCoroutine { continuation ->
        firestore.collection("employees").get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    Employee(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        joiningDate = doc.getString("joiningDate") ?: "",
                        monthlySalary = doc.getDouble("monthlySalary") ?: 0.0,
                        profilePhotoUrl = doc.getString("profilePhotoUrl"),
                        attendancePercentage = doc.getDouble("attendancePercentage") ?: 0.0,
                        role = com.laiza.worker.domain.models.Role.fromFirestore(doc.getString("role")),
                        creditBalance = doc.getDouble("creditBalance") ?: 0.0
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }

    private suspend fun saveEmployeeToFirestore(employee: Employee): Unit = suspendCancellableCoroutine { continuation ->
        val data = hashMapOf(
            "id" to employee.id,
            "name" to employee.name,
            "phone" to employee.phone,
            "joiningDate" to employee.joiningDate,
            "monthlySalary" to employee.monthlySalary,
            "profilePhotoUrl" to (employee.profilePhotoUrl ?: ""),
            "attendancePercentage" to employee.attendancePercentage,
            "role" to employee.role.name,
            "password" to "123123",
            "creditBalance" to employee.creditBalance
        )
        firestore.collection("employees").document(employee.phone).set(data)
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun deleteEmployeeFromFirestore(phone: String): Unit = suspendCancellableCoroutine { continuation ->
        firestore.collection("employees").document(phone).delete()
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }
}
