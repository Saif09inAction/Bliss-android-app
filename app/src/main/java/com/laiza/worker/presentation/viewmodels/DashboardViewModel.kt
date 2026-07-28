package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.domain.models.*
import com.laiza.worker.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val employeeRepository: EmployeeRepository,
    private val attendanceRepository: AttendanceRepository,
    private val paymentRepository: PaymentRepository,
    private val inventoryRepository: InventoryRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val employeeSession = sessionManager.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Admin Stats (used by staff dashboard summaries)
    val totalWorkersCount: StateFlow<Int> = employeeRepository.getAllEmployees()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayAttendanceRate: StateFlow<String> = combine(
        employeeRepository.getAllEmployees(),
        attendanceRepository.getTodayAttendance()
    ) { employees, todayAttendance ->
        if (employees.isEmpty()) "0%"
        else {
            val presents = todayAttendance.count { 
                it.status == AttendanceStatus.PRESENT || 
                it.status == AttendanceStatus.ON_TIME || 
                it.status == AttendanceStatus.LATE ||
                it.status == AttendanceStatus.LEFT_EARLY
            }
            val percentage = (presents.toDouble() / employees.size) * 100
            "${percentage.toInt()}%"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0%")

    val lowStockCount: StateFlow<Int> = inventoryRepository.getAllRawMaterials()
        .map { list -> list.count { it.quantity <= it.minimumStock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalPendingDues: StateFlow<String> = employeeRepository.getAllEmployees()
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf("₹0")
            } else {
                val balanceSheetFlows = list.map { paymentRepository.getSalaryBalanceSheet(it.phone) }
                combine(balanceSheetFlows) { sheets ->
                    val sum = sheets.sumOf { it.salaryRemaining }
                    if (sum >= 100000) {
                        val lakhs = sum / 100000.0
                        String.format(Locale.getDefault(), "₹%.1fL", lakhs)
                    } else {
                        "₹${sum.toInt()}"
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹0")

    // Employee Stats & Alerts
    @OptIn(ExperimentalCoroutinesApi::class)
    val employeeAttendanceStats: StateFlow<Map<String, Int>> = employeeSession
        .flatMapLatest { session ->
            if (session != null) {
                attendanceRepository.getEmployeeAttendanceHistory(session.phone).map { list ->
                    val presents = list.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.ON_TIME }
                    val lates = list.count { it.status == AttendanceStatus.LATE }
                    val earlyOuts = list.count { it.status == AttendanceStatus.LEFT_EARLY }
                    mapOf("presents" to presents, "lates" to lates, "earlyOuts" to earlyOuts)
                }
            } else {
                flowOf(mapOf("presents" to 0, "lates" to 0, "earlyOuts" to 0))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mapOf("presents" to 0, "lates" to 0, "earlyOuts" to 0))

    @OptIn(ExperimentalCoroutinesApi::class)
    val employeeNotifications: StateFlow<List<NotificationAlert>> = employeeSession
        .flatMapLatest { session ->
            if (session != null) {
                notificationRepository.getNotifications(session.phone)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val employeeTodayAttendanceStatus: StateFlow<String> = employeeSession
        .flatMapLatest { session ->
            if (session != null) {
                attendanceRepository.getEmployeeAttendanceHistory(session.phone).map { history ->
                    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayRecord = history.find { it.date == todayDateStr }
                    if (todayRecord != null) {
                        "Checked In at ${todayRecord.signInTime ?: "--"}"
                    } else {
                        "Not Checked In"
                    }
                }
            } else {
                flowOf("Not Checked In")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Not Checked In")

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    fun postAlertNotification(title: String, message: String) {
        viewModelScope.launch {
            val phone = employeeSession.value?.phone ?: return@launch
            val alert = NotificationAlert(
                id = UUID.randomUUID().toString(),
                employeeId = phone,
                title = title,
                message = message,
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                isRead = false
            )
            notificationRepository.addNotification(alert)
        }
    }
}
