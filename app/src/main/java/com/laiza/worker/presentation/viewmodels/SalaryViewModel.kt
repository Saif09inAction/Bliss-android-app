package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.domain.models.PaymentTransaction
import com.laiza.worker.domain.models.SalaryBalanceSheet
import com.laiza.worker.domain.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SalaryViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val userSession = sessionManager.userSession

    @OptIn(ExperimentalCoroutinesApi::class)
    val salaryBalanceSheet: StateFlow<SalaryBalanceSheet> = userSession
        .flatMapLatest { session ->
            if (session != null) {
                paymentRepository.getSalaryBalanceSheet(session.phone)
            } else {
                flowOf(SalaryBalanceSheet("", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SalaryBalanceSheet("", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val paymentHistory: StateFlow<List<PaymentTransaction>> = userSession
        .flatMapLatest { session ->
            if (session != null) {
                paymentRepository.getPaymentsForEmployee(session.phone)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
