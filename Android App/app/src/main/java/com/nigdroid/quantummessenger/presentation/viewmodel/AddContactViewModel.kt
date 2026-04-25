package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.domain.usecase.AddContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddContactUiState {
    object Idle : AddContactUiState()
    object Loading : AddContactUiState()
    data class Success(val fingerprint: String) : AddContactUiState()
    data class AlreadyExists(val fingerprint: String) : AddContactUiState()
    data class Error(val message: String) : AddContactUiState()
    object SelfAdd : AddContactUiState()
}

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val addContactUseCase: AddContactUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddContactUiState>(AddContactUiState.Idle)
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()

    // Debounce: prevent duplicate scans within 2 seconds
    private var lastScannedFingerprint: String? = null
    private var lastScanTime = 0L

    fun addContact(fingerprint: String) {
        val now = System.currentTimeMillis()
        if (fingerprint == lastScannedFingerprint && now - lastScanTime < 2000) return
        lastScannedFingerprint = fingerprint
        lastScanTime = now

        viewModelScope.launch {
            _uiState.value = AddContactUiState.Loading
            _uiState.value = when (val result = addContactUseCase(fingerprint)) {
                is AddContactUseCase.Result.Success       -> AddContactUiState.Success(result.fingerprint)
                is AddContactUseCase.Result.AlreadyExists -> AddContactUiState.AlreadyExists(result.fingerprint)
                is AddContactUseCase.Result.Error         -> AddContactUiState.Error(result.message)
                is AddContactUseCase.Result.SelfAdd       -> AddContactUiState.SelfAdd
            }
        }
    }

    fun resetState() {
        _uiState.value = AddContactUiState.Idle
        lastScannedFingerprint = null
    }
}
