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

// ── Action result states ─────────────────────────────────────────────────────

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

    /** Real Base64-encoded ML-KEM public key from registration. */
    private val _mlKemPublicKey = MutableStateFlow("Loading…")
    val mlKemPublicKey: StateFlow<String> = _mlKemPublicKey.asStateFlow()

    /** Real Base64-encoded X25519 public key from registration. */
    private val _x25519PublicKey = MutableStateFlow("Loading…")
    val x25519PublicKey: StateFlow<String> = _x25519PublicKey.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Whether the display name is in edit mode */
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    /** Account action (logout / delete) state */
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
            _isEditing.value = false  // Switch back to view mode
        }
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    /**
     * Logout — clears the registration flag so the app shows the AuthScreen.
     * Does NOT wipe local data (keys, messages, contacts, database remain intact).
     * Does NOT delete the account on the backend.
     * The user can log back in by tapping "Generate Identity" again,
     * which will detect existing keys and re-authenticate with the same fingerprint.
     */
    fun logout() {
        viewModelScope.launch {
            _accountAction.value = AccountActionState.Loading
            try {
                sessionManager.setUserRegistered(false)
                _accountAction.value = AccountActionState.Success
            } catch (e: Exception) {
                _accountAction.value = AccountActionState.Error(
                    e.message ?: "Logout failed"
                )
            }
        }
    }

    // ── Delete Account ───────────────────────────────────────────────────────

    /**
     * Delete Account — soft-deletes the account on the backend
     * (wipes public keys, marks as deleted), then wipes all local data.
     * Contacts will see "Deleted Account" when they look up this identity.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _accountAction.value = AccountActionState.Loading
            try {
                val success = authRepository.deleteAccount()
                if (success) {
                    _accountAction.value = AccountActionState.Success
                    // Kill the process after a short delay to allow the UI
                    // to navigate. This ensures all stale Hilt singletons
                    // (Room DB, WebSocket, etc.) are fully destroyed.
                    // On next launch, Hilt creates everything fresh.
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

    /** Reset the action state after the UI has consumed it */
    fun resetAccountAction() {
        _accountAction.value = AccountActionState.Idle
    }
}
