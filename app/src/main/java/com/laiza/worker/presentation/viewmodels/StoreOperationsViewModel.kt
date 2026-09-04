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
import com.laiza.worker.core.utils.DateFormatter
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

    val marketplaceCompanies: StateFlow<List<MarketplaceCompany>> =
        storeOperationsRepository.getMarketplaceCompanies()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordPickup(
        clarisQuantity: Int,
        blissQuantity: Int,
        platform: String,
        deliveryPartner: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val claris = clarisQuantity.coerceAtLeast(0)
            val bliss = blissQuantity.coerceAtLeast(0)
            if (claris + bliss <= 0) {
                onError("Enter Claris and/or Bliss quantity")
                return@launch
            }
            if (platform.isBlank()) {
                onError("Select a company")
                return@launch
            }
            if (deliveryPartner.isBlank()) {
                onError("Select a delivery partner")
                return@launch
            }
            val session = sessionManager.userSession.firstOrNull()
            val now = Date()
            val record = PickupRecord(
                quantity = claris + bliss,
                clarisQuantity = claris,
                blissQuantity = bliss,
                partner = platform.trim(),
                deliveryPartner = DeliveryPartnerDefaults.normalize(deliveryPartner.trim()),
                staffId = session?.phone ?: "",
                staffName = session?.name ?: "Staff",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                time = DateFormatter.nowTime12Hour()
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
        clarisQuantity: Int,
        blissQuantity: Int,
        platform: String,
        deliveryPartner: String,
        returnType: ReturnType,
        notes: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val claris = clarisQuantity.coerceAtLeast(0)
            val bliss = blissQuantity.coerceAtLeast(0)
            if (claris + bliss <= 0) {
                onError("Enter Claris and/or Bliss quantity")
                return@launch
            }
            if (platform.isBlank()) {
                onError("Select a company")
                return@launch
            }
            if (deliveryPartner.isBlank()) {
                onError("Select a delivery partner")
                return@launch
            }
            val session = sessionManager.userSession.firstOrNull()
            val now = Date()
            val record = ReturnRecord(
                quantity = claris + bliss,
                clarisQuantity = claris,
                blissQuantity = bliss,
                partner = platform.trim(),
                deliveryPartner = DeliveryPartnerDefaults.normalize(deliveryPartner.trim()),
                returnType = returnType,
                staffId = session?.phone ?: "",
                staffName = session?.name ?: "Staff",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
                time = DateFormatter.nowTime12Hour(),
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
