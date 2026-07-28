package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.RawMaterial
import com.laiza.worker.domain.repository.InventoryRepository
import com.laiza.worker.core.session.SessionManager
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

    val rawMaterials: StateFlow<List<RawMaterial>> = _searchQuery
        .combine(inventoryRepository.getAllRawMaterials()) { query, list ->
            if (query.isBlank()) {
                list
            } else {
                list.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
