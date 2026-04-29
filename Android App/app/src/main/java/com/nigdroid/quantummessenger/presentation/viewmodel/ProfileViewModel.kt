package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.data.security.VaultWipeManager
import com.nigdroid.quantummessenger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AccountActionState {
    object Idle : AccountActionState()
    object Loading : AccountActionState()
    object Success : AccountActionState()
    data class Error(val message: String) : AccountActionState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val vaultWipeManager: VaultWipeManager
) : ViewModel() {

    private val _fingerprint = MutableStateFlow<String?>(null)
    val fingerprint: StateFlow<String?> = _fingerprint.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _mlKemPublicKey = MutableStateFlow("Loading…")
    val mlKemPublicKey: StateFlow<String> = _mlKemPublicKey.asStateFlow()

    private val _x25519PublicKey = MutableStateFlow("Loading…")
    val x25519PublicKey: StateFlow<String> = _x25519PublicKey.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _accountAction = MutableStateFlow<AccountActionState>(AccountActionState.Idle)
    val accountAction: StateFlow<AccountActionState> = _accountAction.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            sessionManager.textFingerprint.collect { fp ->
                _fingerprint.value = fp
            }
        }
        viewModelScope.launch {
            sessionManager.displayName.collect { name ->
                _displayName.value = name
            }
        }
        viewModelScope.launch {
            sessionManager.mlKemPublicKey.collect { key ->
                _mlKemPublicKey.value = key ?: "Not yet generated"
            }
        }
        viewModelScope.launch {
            sessionManager.x25519PublicKey.collect { key ->
                _x25519PublicKey.value = key ?: "Not yet generated"
            }
        }
    }

    fun saveDisplayName(name: String) {
        viewModelScope.launch {
            _isSaving.value = true
            sessionManager.setDisplayName(name.ifBlank { null })
            _isSaving.value = false
            _isEditing.value = false
        }
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _accountAction.value = AccountActionState.Loading
            try {
                val success = authRepository.deleteAccount()
                if (success) {
                    _accountAction.value = AccountActionState.Success
                    kotlinx.coroutines.delay(500)
                    android.os.Process.killProcess(android.os.Process.myPid())
                } else {
                    _accountAction.value = AccountActionState.Error(
                        "Failed to delete account on server. Please try again."
                    )
                }
            } catch (e: Exception) {
                _accountAction.value = AccountActionState.Error(
                    e.message ?: "Delete account failed"
                )
            }
        }
    }

    fun resetAccountAction() {
        _accountAction.value = AccountActionState.Idle
    }
}
