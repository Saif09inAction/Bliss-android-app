package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.FinishedProduct
import com.laiza.worker.domain.models.RawMaterial
import com.laiza.worker.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _productSearch = MutableStateFlow("")
    val productSearch = _productSearch.asStateFlow()

    val rawMaterials: StateFlow<List<RawMaterial>> = _searchQuery
        .combine(inventoryRepository.getAllRawMaterials()) { query, list ->
            if (query.isBlank()) list
            else list.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finishedProducts: StateFlow<List<FinishedProduct>> =
        inventoryRepository.getAllFinishedProducts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFinishedProducts: StateFlow<List<FinishedProduct>> =
        productSearch.combine(finishedProducts) { query, list ->
            if (query.isBlank()) list
            else list.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.color.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onProductSearchChange(query: String) {
        _productSearch.value = query
    }

    fun refreshFinishedProducts(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            inventoryRepository.refreshFinishedProducts().collect { res ->
                when (res) {
                    is Resource.Success -> onComplete?.invoke(true)
                    is Resource.Error -> onComplete?.invoke(false)
                    else -> {}
                }
            }
        }
    }

    fun addManualInventory(
        name: String,
        color: String,
        quantity: Int,
        unitPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val session = sessionManager.userSession.firstOrNull()
            inventoryRepository.addManualFinishedProduct(
                name = name,
                color = color,
                quantity = quantity,
                unitPrice = unitPrice,
                updatedBy = session?.name ?: "Staff"
            ).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed to add inventory")
                    else -> {}
                }
            }
        }
    }

    fun addRawMaterial(material: RawMaterial, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            inventoryRepository.addRawMaterial(material).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed to add raw material")
                    else -> {}
                }
            }
        }
    }

    fun updateRawMaterial(material: RawMaterial, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            inventoryRepository.updateRawMaterial(material).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed to update raw material")
                    else -> {}
                }
            }
        }
    }

    fun deleteRawMaterial(id: String, materialName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            inventoryRepository.deleteRawMaterial(id).collect { res ->
                when (res) {
                    is Resource.Success -> onSuccess()
                    is Resource.Error -> onError(res.message ?: "Failed to delete raw material")
                    else -> {}
                }
            }
        }
    }
}
