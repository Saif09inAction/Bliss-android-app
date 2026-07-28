package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.*
import com.laiza.worker.domain.repository.AttendanceRepository
import com.laiza.worker.domain.repository.EmployeeRepository
import com.laiza.worker.domain.repository.PaymentRepository
import com.laiza.worker.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmployeeWithAdvance(
    val employee: Employee,
    val totalAdvance: Double
)

data class EmployeeWithPayroll(
    val employee: Employee,
    val totalAdvance: Double,
    val currentMonthPending: Double,
    val isPaidThisMonth: Boolean,
    val totalPaidTillDate: Double,
    val advanceRemaining: Double
)

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _searchQuery
        .combine(employeeRepository.getAllEmployees()) { query, list ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.phone.contains(query)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeesWithAdvance: StateFlow<List<EmployeeWithAdvance>> = employees
        .combine(paymentRepository.getAllTransactions()) { employeeList, transactions ->
            employeeList.map { emp ->
                val totalAdv = transactions
                    .filter { (it.employeeId == emp.id || it.employeeId == emp.phone) && it.type == PaymentType.ADVANCE }
                    .sumOf { it.amount }
                EmployeeWithAdvance(emp, totalAdv)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeesWithPayroll: StateFlow<List<EmployeeWithPayroll>> = employees
        .combine(paymentRepository.getAllTransactions()) { employeeList, transactions ->
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonthKey = sdf.format(Date())

            employeeList.map { emp ->
                val empTx = transactions.filter { it.employeeId == emp.id || it.employeeId == emp.phone }
                
                val currentMonthPaid = empTx.filter {
                    it.type == PaymentType.SALARY_PAYMENT && it.date.startsWith(currentMonthKey)
                }.sumOf { it.amount }

                val currentMonthPending = maxOf(0.0, emp.monthlySalary - currentMonthPaid)
                val isPaidThisMonth = currentMonthPending <= 0.0

                val totalPaidTillDate = empTx.filter {
                    it.type == PaymentType.SALARY_PAYMENT
                }.sumOf { it.amount }

                val advanceTaken = empTx.filter {
                    it.type == PaymentType.ADVANCE
                }.sumOf { it.amount }

                val advanceDeducted = empTx.filter {
                    it.type == PaymentType.DEDUCTION
                }.sumOf { it.amount }

                val advanceRemaining = maxOf(0.0, advanceTaken - advanceDeducted)

                EmployeeWithPayroll(
                    employee = emp,
                    totalAdvance = advanceTaken,
                    currentMonthPending = currentMonthPending,
                    isPaidThisMonth = isPaidThisMonth,
                    totalPaidTillDate = totalPaidTillDate,
                    advanceRemaining = advanceRemaining
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEmployeePhone = MutableStateFlow<String?>(null)
    val selectedEmployeePhone = _selectedEmployeePhone.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedEmployee: StateFlow<Employee?> = _selectedEmployeePhone
        .flatMapLatest { phone ->
            if (phone != null) employeeRepository.getEmployee(phone) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedEmployeeAttendance: StateFlow<List<Attendance>> = _selectedEmployeePhone
        .flatMapLatest { phone ->
            if (phone != null) attendanceRepository.getEmployeeAttendanceHistory(phone) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedEmployeePayments: StateFlow<List<PaymentTransaction>> = _selectedEmployeePhone
        .flatMapLatest { phone ->
            if (phone != null) paymentRepository.getPaymentsForEmployee(phone) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedEmployeeBalance: StateFlow<SalaryBalanceSheet> = _selectedEmployeePhone
        .flatMapLatest { phone ->
            if (phone != null) paymentRepository.getSalaryBalanceSheet(phone) else flowOf(SalaryBalanceSheet("", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalaryBalanceSheet("", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectEmployee(phone: String?) {
        _selectedEmployeePhone.value = phone
    }

    fun addEmployee(employee: Employee, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            employeeRepository.addEmployee(employee).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> {
                        onError(res.message ?: "Failed to save employee")
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateEmployee(employee: Employee, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            employeeRepository.updateEmployee(employee).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> {
                        onError(res.message ?: "Failed to update employee")
                    }
                    else -> {}
                }
            }
        }
    }

    fun deleteEmployee(id: String, employeeName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            employeeRepository.deleteEmployee(id).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> {
                        onError(res.message ?: "Failed to delete employee")
                    }
                    else -> {}
                }
            }
        }
    }

    fun addPayment(payment: PaymentTransaction, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val userName = sessionManager.userSession.firstOrNull()?.name ?: "Admin"
            paymentRepository.addPayment(
                employeeId = payment.employeeId,
                amount = payment.amount,
                type = payment.type,
                remarks = payment.remarks,
                adminName = userName
            ).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> {
                        onError(res.message ?: "Failed to record payment transaction")
                    }
                    else -> {}
                }
            }
        }
    }

    private val _selectedEmployeeExtraProfile = MutableStateFlow<EmployeeExtraProfile?>(null)
    val selectedEmployeeExtraProfile = _selectedEmployeeExtraProfile.asStateFlow()

    private var extraProfileListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadEmployeeExtraProfile(phone: String) {
        extraProfileListener?.remove()
        try {
            extraProfileListener = FirebaseFirestore.getInstance().collection("employee_profiles").document(phone)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val profile = EmployeeExtraProfile(
                            phone = phone,
                            email = snapshot.getString("email") ?: "",
                            address = snapshot.getString("address") ?: "",
                            designation = snapshot.getString("designation") ?: "",
                            employeeId = snapshot.getString("employeeId") ?: "",
                            accountHolder = snapshot.getString("accountHolder") ?: "",
                            bankName = snapshot.getString("bankName") ?: "",
                            accountNumber = snapshot.getString("accountNumber") ?: "",
                            ifscCode = snapshot.getString("ifscCode") ?: "",
                            branch = snapshot.getString("branch") ?: "",
                            upiId = snapshot.getString("upiId") ?: ""
                        )
                        _selectedEmployeeExtraProfile.value = profile
                    } else {
                        _selectedEmployeeExtraProfile.value = EmployeeExtraProfile(phone = phone)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _selectedEmployeeExtraProfile.value = EmployeeExtraProfile(phone = phone)
        }
    }

    fun saveEmployeeExtraProfile(profile: EmployeeExtraProfile, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "email" to profile.email,
                    "address" to profile.address,
                    "designation" to profile.designation,
                    "employeeId" to profile.employeeId,
                    "accountHolder" to profile.accountHolder,
                    "bankName" to profile.bankName,
                    "accountNumber" to profile.accountNumber,
                    "ifscCode" to profile.ifscCode,
                    "branch" to profile.branch,
                    "upiId" to profile.upiId
                )
                FirebaseFirestore.getInstance().collection("employee_profiles").document(profile.phone).set(data)
                    .addOnSuccessListener {
                        _selectedEmployeeExtraProfile.value = profile
                        onSuccess()
                    }
                    .addOnFailureListener { err ->
                        onError(err.message ?: "Failed to save details")
                    }
            } catch (e: Exception) {
                onError(e.message ?: "Error saving details")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        extraProfileListener?.remove()
    }
}
