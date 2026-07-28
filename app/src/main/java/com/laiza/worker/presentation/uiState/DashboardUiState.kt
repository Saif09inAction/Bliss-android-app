package com.laiza.worker.presentation.uiState

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    object Success : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
