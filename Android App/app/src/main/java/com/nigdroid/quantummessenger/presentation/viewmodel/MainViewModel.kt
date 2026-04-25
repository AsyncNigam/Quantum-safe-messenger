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

// ─── Lock State Machine ───────────────────────────────────────────────────────

sealed class LockState {
    /** App is locked — LockedScreen shown, awaiting biometric */
    object Locked : LockState()

    /** Biometric succeeded and key is intact — app accessible */
    object Unlocked : LockState()

    /** Biometric succeeded but Keystore key is permanently invalidated.
     *  User must wipe vault and re-register. */
    object VaultCompromised : LockState()

    /** Vault wipe is in progress */
    object Wiping : LockState()

    /** Wipe complete — ready to navigate to AuthScreen */
    object WipeComplete : LockState()

    /** Biometric prompt encountered an error */
    data class BiometricError(val message: String) : LockState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val cryptoManager: CryptoManager,
    private val vaultWipeManager: VaultWipeManager
) : ViewModel() {

    // ── Navigation destination (Auth or Home) ─────────────────────────────────
    private val _startDestination = MutableStateFlow<Any?>(null)
    val startDestination: StateFlow<Any?> = _startDestination.asStateFlow()

    // ── Lock state ────────────────────────────────────────────────────────────
    private val _lockState = MutableStateFlow<LockState>(LockState.Locked)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    init {
        checkRegistrationStatus()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Called when biometric authentication succeeds.
     * Validates that the Keystore master key is still intact.
     * If the key has been invalidated (biometric enrollment changed),
     * transitions to VaultCompromised state.
     */
    fun onBiometricSuccess() {
        viewModelScope.launch {
            val keyValid = cryptoManager.validateKeyIntegrity()
            _lockState.value = if (keyValid) {
                // Ensure master key exists (safe to call — idempotent)
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

    /**
     * Called when biometric authentication fails or errors out.
     */
    fun onBiometricError(message: String) {
        _lockState.value = LockState.BiometricError(message)
    }

    /**
     * Called when biometric is unavailable on the device.
     * Falls back to unlocked (device has no biometric hardware).
     */
    fun onBiometricUnavailable() {
        viewModelScope.launch {
            // Still check key integrity even without biometric gate
            val keyValid = cryptoManager.validateKeyIntegrity()
            _lockState.value = if (keyValid) LockState.Unlocked else LockState.VaultCompromised
        }
    }

    /**
     * Lock the app — called on every ON_STOP (app backgrounded).
     */
    fun lock() {
        // Only lock if we were unlocked (don't override VaultCompromised or Wiping)
        if (_lockState.value == LockState.Unlocked) {
            _lockState.value = LockState.Locked
        }
    }

    /**
     * Retry biometric after an error.
     */
    fun retryBiometric() {
        _lockState.value = LockState.Locked
    }

    /**
     * Execute the zero-trust vault wipe.
     * Deletes all local data and routes to AuthScreen for re-registration.
     */
    fun executeVaultWipe() {
        viewModelScope.launch {
            _lockState.value = LockState.Wiping
            vaultWipeManager.executeZeroTrustWipe()
            _startDestination.value = AuthRoute
            _lockState.value = LockState.WipeComplete
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun checkRegistrationStatus() {
        viewModelScope.launch {
            sessionManager.isUserRegistered.collect { isRegistered ->
                _startDestination.value = if (isRegistered) {
                    HomeRoute
                } else {
                    AuthRoute
                }
            }
        }
    }
}
