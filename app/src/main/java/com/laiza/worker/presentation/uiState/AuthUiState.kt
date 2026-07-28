package com.laiza.worker.presentation.uiState

import com.laiza.worker.domain.models.UserSession

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val session: UserSession) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

data class LoginScreenState(
    val employeeId: String = "",
    val password: String = "",
    val employeeIdError: String? = null,
    val passwordError: String? = null,
    val rememberMe: Boolean = false,
    val isLoginButtonEnabled: Boolean = false,
    val selectedRole: com.laiza.worker.domain.models.Role = com.laiza.worker.domain.models.Role.STAFF
)
