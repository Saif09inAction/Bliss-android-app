package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.core.utils.ValidationHelper
import com.laiza.worker.domain.models.Role
import com.laiza.worker.domain.usecases.CheckSessionUseCase
import com.laiza.worker.domain.usecases.LoginUseCase
import com.laiza.worker.domain.usecases.LogoutUseCase
import com.laiza.worker.presentation.uiState.AuthUiState
import com.laiza.worker.presentation.uiState.LoginScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val checkSessionUseCase: CheckSessionUseCase,
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _loginScreenState = MutableStateFlow(LoginScreenState())
    val loginScreenState: StateFlow<LoginScreenState> = _loginScreenState.asStateFlow()

    val userSession = sessionManager.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private var accountWatchJob: Job? = null

    init {
        // Real-time: if admin deletes this employee, force logout immediately
        accountWatchJob = viewModelScope.launch {
            sessionManager.userSession.collectLatest { session ->
                if (session == null) return@collectLatest
                callbackFlow {
                    val registration = firestore.collection("employees")
                        .document(session.phone)
                        .addSnapshotListener { snap, error ->
                            if (error != null) return@addSnapshotListener
                            trySend(snap != null && snap.exists())
                        }
                    awaitClose { registration.remove() }
                }.collect { exists ->
                    if (!exists) {
                        logoutUseCase().collect { }
                        _authUiState.value = AuthUiState.Idle
                        _loginScreenState.value = LoginScreenState()
                    }
                }
            }
        }
    }

    suspend fun resolveStartupSession() = checkSessionUseCase().firstOrNull()

    fun homeRouteForRole(role: Role): String {
        return when (role) {
            Role.KAARIGER -> com.laiza.worker.core.navigation.Screen.KaarigerGraph.route
            Role.STAFF, Role.ADMIN -> com.laiza.worker.core.navigation.Screen.StaffGraph.route
        }
    }

    fun onEmployeeIdChange(value: String) {
        _loginScreenState.update { state ->
            val error = if (value.trim().isEmpty()) {
                "Mobile number is required"
            } else {
                null
            }
            state.copy(
                employeeId = value,
                employeeIdError = error,
                isLoginButtonEnabled = validateInputs(value, state.password)
            )
        }
    }

    fun onPasswordChange(value: String) {
        _loginScreenState.update { state ->
            val error = when {
                value.trim().isEmpty() -> "Password is required"
                !ValidationHelper.isValidPassword(value) -> "Password must be at least 6 characters"
                else -> null
            }
            state.copy(
                password = value,
                passwordError = error,
                isLoginButtonEnabled = validateInputs(state.employeeId, value)
            )
        }
    }

    fun onRememberMeChange(value: Boolean) {
        _loginScreenState.update { it.copy(rememberMe = value) }
    }

    fun onRoleChange(role: Role) {
        _loginScreenState.update { it.copy(selectedRole = role) }
    }

    private fun validateInputs(employeeId: String, password: String): Boolean {
        return employeeId.trim().isNotEmpty() &&
                password.trim().isNotEmpty() &&
                ValidationHelper.isValidPassword(password)
    }

    fun login() {
        val currentState = _loginScreenState.value
        if (!validateInputs(currentState.employeeId, currentState.password)) {
            return
        }

        viewModelScope.launch {
            loginUseCase(currentState.currentStateValuePhone(), currentState.password, currentState.selectedRole).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _authUiState.value = AuthUiState.Loading
                    }
                    is Resource.Success -> {
                        _authUiState.value = AuthUiState.Success(resource.data!!)
                    }
                    is Resource.Error -> {
                        _authUiState.value = AuthUiState.Error(resource.message ?: "Login failed")
                    }
                }
            }
        }
    }

    private fun LoginScreenState.currentStateValuePhone(): String {
        return employeeId.trim()
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase().collect { resource ->
                if (resource is Resource.Success) {
                    _authUiState.value = AuthUiState.Idle
                    _loginScreenState.value = LoginScreenState()
                }
            }
        }
    }

    fun resetUiState() {
        _authUiState.value = AuthUiState.Idle
    }

    override fun onCleared() {
        accountWatchJob?.cancel()
        super.onCleared()
    }
}
