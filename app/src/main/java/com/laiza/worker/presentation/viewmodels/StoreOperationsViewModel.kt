package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.*
import com.laiza.worker.domain.repository.StoreOperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StoreOperationsViewModel @Inject constructor(
    private val storeOperationsRepository: StoreOperationsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val allPickups: StateFlow<List<PickupRecord>> = storeOperationsRepository.getAllPickups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReturns: StateFlow<List<ReturnRecord>> = storeOperationsRepository.getAllReturns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveryPartners: StateFlow<List<DeliveryPartner>> =
        storeOperationsRepository.getDeliveryPartners()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDeliveryPartner(
        name: String,
        onSuccess: (DeliveryPartner) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            storeOperationsRepository.addDeliveryPartner(name).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess(res.data!!)
                    is Resource.Error -> onError(res.message ?: "Failed to add partner")
                    else -> {}
                }
            }
        }
    }

    fun recordPickup(
        quantity: Int,
        platform: String,
        deliveryPartner: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (quantity <= 0) {
                onError("Enter a valid quantity")
                return@launch
            }
            if (platform.isBlank()) {
                onError("Select a marketplace (Amazon, Flipkart…)")
                return@launch
            }
            if (deliveryPartner.isBlank()) {
                onError("Select or add a delivery partner")
                return@launch
            }
            val session = sessionManager.userSession.firstOrNull()
            val now = Date()
            val record = PickupRecord(
                quantity = quantity,
                partner = platform.trim(),
                deliveryPartner = deliveryPartner.trim(),
                staffId = session?.phone ?: "",
                staffName = session?.name ?: "Staff",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            )
            storeOperationsRepository.recordPickup(record).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }

    fun recordReturn(
        quantity: Int,
        platform: String,
        deliveryPartner: String,
        returnType: ReturnType,
        notes: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (quantity <= 0) {
                onError("Enter a valid quantity")
                return@launch
            }
            if (platform.isBlank()) {
                onError("Select a marketplace (Amazon, Flipkart…)")
                return@launch
            }
            if (deliveryPartner.isBlank()) {
                onError("Select or add a delivery partner")
                return@launch
            }
            val session = sessionManager.userSession.firstOrNull()
            val now = Date()
            val record = ReturnRecord(
                quantity = quantity,
                partner = platform.trim().ifBlank { EcommercePlatform.FLIPKART },
                deliveryPartner = deliveryPartner.trim(),
                returnType = returnType,
                staffId = session?.phone ?: "",
                staffName = session?.name ?: "Staff",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                notes = notes
            )
            storeOperationsRepository.recordReturn(record).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed")
                    else -> {}
                }
            }
        }
    }
}
