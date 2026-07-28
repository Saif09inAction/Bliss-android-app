package com.laiza.worker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laiza.worker.core.session.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KaarigerLanguageViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    val language = dataStoreManager.kaarigerLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    fun setLanguage(code: String) {
        viewModelScope.launch {
            dataStoreManager.saveKaarigerLanguage(code)
        }
    }
}
