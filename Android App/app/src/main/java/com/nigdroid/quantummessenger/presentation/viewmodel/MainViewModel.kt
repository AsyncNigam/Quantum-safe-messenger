package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.crypto.CryptoManager
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.data.security.VaultWipeManager
import com.nigdroid.quantummessenger.presentation.navigation.AuthRoute
import com.nigdroid.quantummessenger.presentation.navigation.HomeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LockState {
    object Locked : LockState()
    object Unlocked : LockState()
    object VaultCompromised : LockState()
    object Wiping : LockState()
    object WipeComplete : LockState()
    data class BiometricError(val message: String) : LockState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val cryptoManager: CryptoManager,
    private val vaultWipeManager: VaultWipeManager
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Any?>(null)
    val startDestination: StateFlow<Any?> = _startDestination.asStateFlow()

    private val _lockState = MutableStateFlow<LockState>(LockState.Locked)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    init {
        checkRegistrationStatus()
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            val keyValid = cryptoManager.validateKeyIntegrity()
            _lockState.value = if (keyValid) {
                try {
                    cryptoManager.ensureAuthBoundMasterKey()
                } catch (e: Exception) {
                    android.util.Log.e("MainVM", "Key init error: ${e.message}")
                }
                LockState.Unlocked
            } else {
                LockState.VaultCompromised
            }
        }
    }

    fun onBiometricError(message: String) {
        _lockState.value = LockState.BiometricError(message)
    }

    fun onBiometricUnavailable() {
        viewModelScope.launch {
            val keyValid = cryptoManager.validateKeyIntegrity()
            _lockState.value = if (keyValid) LockState.Unlocked else LockState.VaultCompromised
        }
    }

    fun lock() {
        if (_lockState.value == LockState.Unlocked) {
            _lockState.value = LockState.Locked
        }
    }

    fun retryBiometric() {
        _lockState.value = LockState.Locked
    }

    fun executeVaultWipe() {
        viewModelScope.launch {
            _lockState.value = LockState.Wiping
            vaultWipeManager.executeZeroTrustWipe()
            _startDestination.value = AuthRoute
            _lockState.value = LockState.WipeComplete
        }
    }

    private fun checkRegistrationStatus() {
        viewModelScope.launch {
            sessionManager.isUserRegistered.collect { isRegistered ->
                _startDestination.value = if (isRegistered) HomeRoute else AuthRoute
            }
        }
    }
}
