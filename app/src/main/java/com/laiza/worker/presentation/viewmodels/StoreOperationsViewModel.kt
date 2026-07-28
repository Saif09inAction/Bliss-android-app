package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.*
import com.laiza.worker.domain.repository.InventoryRepository
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
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _inventorySearch = MutableStateFlow("")
    val inventorySearch = _inventorySearch.asStateFlow()

    val storeInventory: StateFlow<List<FinishedProduct>> = inventoryRepository.getAllFinishedProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInventory: StateFlow<List<FinishedProduct>> = inventorySearch
        .combine(storeInventory) { query, list ->
            if (query.isBlank()) list
            else list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.color.contains(query, ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPickups: StateFlow<List<PickupRecord>> = storeOperationsRepository.getAllPickups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReturns: StateFlow<List<ReturnRecord>> = storeOperationsRepository.getAllReturns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onInventorySearchChange(query: String) {
        _inventorySearch.value = query
    }

    fun recordPickup(
        product: FinishedProduct,
        quantity: Int,
        partner: EcommercePartner,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val session = sessionManager.userSession.firstOrNull()
            val now = Date()
            val record = PickupRecord(
                productId = product.id,
                productName = product.name,
                color = product.color,
                quantity = quantity,
                partner = partner,
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
        product: FinishedProduct,
        quantity: Int,
        partner: EcommercePartner,
        returnType: ReturnType,
        notes: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val session = sessionManager.userSession.firstOrNull()
            val now = Date()
            val record = ReturnRecord(
                productId = product.id,
                productName = product.name,
                color = product.color,
                quantity = quantity,
                partner = partner,
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
