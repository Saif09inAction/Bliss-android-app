package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.*
import com.laiza.worker.domain.repository.EmployeeRepository
import com.laiza.worker.domain.repository.InventoryRepository
import com.laiza.worker.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class OrderPaymentSummary(
    val order: KaarigerOrder,
    val totalPaid: Double,
    val remaining: Double
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val employeeRepository: EmployeeRepository,
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val allOrders: StateFlow<List<KaarigerOrder>> = orderRepository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingApprovals: StateFlow<List<KaarigerOrder>> = orderRepository.getPendingApprovalOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingApprovalCount: StateFlow<Int> = pendingApprovals
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _staffApprovalHistory = MutableStateFlow<List<OrderApprovalRecord>>(emptyList())
    val staffApprovalHistory = _staffApprovalHistory.asStateFlow()

    val kaarigers: StateFlow<List<Employee>> = employeeRepository.getAllEmployees()
        .map { list -> list.filter { it.role == Role.KAARIGER } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawMaterials = inventoryRepository.getAllRawMaterials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storeInventory = inventoryRepository.getAllFinishedProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _kaarigerOrders = MutableStateFlow<List<KaarigerOrder>>(emptyList())
    val kaarigerOrders = _kaarigerOrders.asStateFlow()

    private val _kaarigerPayments = MutableStateFlow<List<KaarigerOrderPayment>>(emptyList())
    val kaarigerPayments = _kaarigerPayments.asStateFlow()

    private val _kaarigerRepairs = MutableStateFlow<List<OrderRepair>>(emptyList())
    val kaarigerRepairs = _kaarigerRepairs.asStateFlow()

    private val _paymentSummaries = MutableStateFlow<List<OrderPaymentSummary>>(emptyList())
    val paymentSummaries = _paymentSummaries.asStateFlow()

    fun loadStaffApprovalHistory(staffPhone: String) {
        viewModelScope.launch {
            orderRepository.getApprovalHistoryForStaff(staffPhone).collect { records ->
                _staffApprovalHistory.value = records
            }
        }
    }

    fun loadKaarigerData(kaarigerId: String) {
        viewModelScope.launch {
            orderRepository.getOrdersForKaariger(kaarigerId).collect { orders ->
                _kaarigerOrders.value = orders
            }
        }
        viewModelScope.launch {
            orderRepository.getPaymentsForKaariger(kaarigerId).collect { payments ->
                _kaarigerPayments.value = payments
                updatePaymentSummaries()
            }
        }
        viewModelScope.launch {
            orderRepository.getRepairsForKaariger(kaarigerId).collect { repairs ->
                _kaarigerRepairs.value = repairs
            }
        }
    }

    fun loadPaymentSummaries() {
        viewModelScope.launch {
            combine(orderRepository.getAllOrders(), orderRepository.getPaymentsForKaariger("")) { _, _ ->
                Unit
            }.collect {
                updatePaymentSummaries()
            }
        }
    }

    private fun updatePaymentSummaries() {
        viewModelScope.launch {
            orderRepository.getAllOrders().first().forEach { order ->
                // summaries updated via combine in UI
            }
        }
    }

    fun getOrderPaymentSummary(order: KaarigerOrder, payments: List<KaarigerOrderPayment>): OrderPaymentSummary {
        val paid = payments.filter { it.orderId == order.id }.sumOf { it.amount }
        val net = order.effectiveDealAmount()
        return OrderPaymentSummary(order, paid, (net - paid).coerceAtLeast(0.0))
    }

    fun createOrder(order: KaarigerOrder, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            orderRepository.createOrder(order).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }

    fun submitDelivery(
        orderId: String,
        quantity: Int,
        color: String,
        productName: String,
        notes: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            orderRepository.submitDelivery(orderId, quantity, color, productName, notes).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }

    fun approveOrder(
        orderId: String,
        acceptedQuantity: Int,
        colorBreakdown: List<ColorQuantity>,
        rejectedQuantity: Int,
        rejectionNote: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val session = sessionManager.userSession.firstOrNull()
            val verifiedBy = session?.name ?: "Staff"
            val verifiedByPhone = session?.phone ?: ""
            orderRepository.approveOrder(
                orderId = orderId,
                acceptedQuantity = acceptedQuantity,
                colorBreakdown = colorBreakdown,
                rejectedQuantity = rejectedQuantity,
                rejectionNote = rejectionNote,
                verifiedBy = verifiedBy,
                verifiedByPhone = verifiedByPhone
            ).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }

    fun rejectOrder(orderId: String, reason: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val verifiedBy = sessionManager.userSession.firstOrNull()?.name ?: "Staff"
            orderRepository.rejectOrder(orderId, verifiedBy, reason).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }

    fun submitMaterialUsage(
        orderId: String,
        materials: List<OrderMaterial>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            orderRepository.submitMaterialUsage(orderId, materials).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }

    fun addAdvancePayment(
        orderId: String,
        kaarigerId: String,
        amount: Double,
        remarks: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val createdBy = sessionManager.userSession.firstOrNull()?.name ?: "Admin"
            val now = Date()
            val payment = KaarigerOrderPayment(
                orderId = orderId,
                kaarigerId = kaarigerId,
                amount = amount,
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                remarks = remarks,
                createdBy = createdBy
            )
            orderRepository.addPayment(payment).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }

    fun getPaymentsForOrder(orderId: String): Flow<List<KaarigerOrderPayment>> {
        return orderRepository.getPaymentsForOrder(orderId)
    }

    fun createRepair(
        orderId: String,
        productName: String,
        faultyQuantity: Int,
        faultyPricePerPiece: Double,
        notes: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val createdBy = sessionManager.userSession.firstOrNull()?.name ?: "Staff"
            orderRepository.createRepair(
                orderId = orderId,
                productName = productName,
                faultyQuantity = faultyQuantity,
                faultyPricePerPiece = faultyPricePerPiece,
                createdBy = createdBy,
                notes = notes
            ).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }
}
