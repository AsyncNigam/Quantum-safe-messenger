package com.nigdroid.quantummessenger.presentation.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.domain.model.Identity
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult
import com.nigdroid.quantummessenger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── State machine ────────────────────────────────────────────────────────────

sealed class AuthState {
    /** Waiting for the user to tap "Generate Anonymous Identity" */
    object Idle : AuthState()

    /** Generating ML-KEM keypair (step 1 of 3) */
    object GeneratingMLKem : AuthState()

    /** Generating X25519 keypair (step 2 of 3) */
    object GeneratingX25519 : AuthState()

    /** Registering identity with the backend (step 3 of 3) */
    object Registering : AuthState()

    /** Registration complete */
    data class Success(val textFingerprint: String, val identity: Identity) : AuthState()

    /** Something went wrong */
    data class Error(
        val message: String,
        val exception: Exception? = null,
        val step: ErrorStep = ErrorStep.UNKNOWN
    ) : AuthState()
}

enum class ErrorStep { KEY_GENERATION, REGISTRATION, UNKNOWN }

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Starts the full Zero-Knowledge identity generation + registration flow.
     * Drives the UI through each animated step.
     */
    fun generateIdentity() {
        viewModelScope.launch {
            try {
                // Step 1
                _authState.value = AuthState.GeneratingMLKem
                kotlinx.coroutines.delay(900L)

                // Step 2
                _authState.value = AuthState.GeneratingX25519
                kotlinx.coroutines.delay(700L)

                // Step 3
                _authState.value = AuthState.Registering

                val result = authRepository.generateAndRegisterIdentity()

                when (result) {
                    is IdentityGenerationResult.Success -> {
                        _authState.value = AuthState.Success(
                            textFingerprint = result.identity.textFingerprint,
                            identity        = result.identity
                        )
                    }
                    is IdentityGenerationResult.Error -> {
                        _authState.value = AuthState.Error(
                            message   = result.message,
                            exception = result.exception,
                            step      = ErrorStep.REGISTRATION
                        )
                    }
                    is IdentityGenerationResult.Cancelled -> {
                        _authState.value = AuthState.Idle
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    message   = "Unexpected error: ${e.message}",
                    exception = e,
                    step      = ErrorStep.UNKNOWN
                )
            }
        }
    }

    fun retry() {
        _authState.value = AuthState.Idle
    }
}
