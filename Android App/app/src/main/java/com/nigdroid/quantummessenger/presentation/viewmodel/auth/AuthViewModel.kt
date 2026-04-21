package com.nigdroid.quantummessenger.presentation.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.domain.model.AuthenticationResult
import com.nigdroid.quantummessenger.domain.model.Identity
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult
import com.nigdroid.quantummessenger.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Authentication State - Sealed class representing all possible UI states
 *
 * Implements Unidirectional Data Flow (UDF) pattern for MVI architecture
 */
sealed class AuthState {
    /** Initial state before any action */
    object Idle : AuthState()

    /** Generating cryptographic keys (heavy CPU operation) */
    data class GeneratingKeys(
        val progress: Int = 0  // 0-100 progress indicator
    ) : AuthState()

    /** Keys generated, uploading to server */
    data class Uploading(
        val identity: Identity
    ) : AuthState()

    /** Successfully registered and authenticated */
    data class Success(
        val userId: String,
        val identity: Identity
    ) : AuthState()

    /** Error occurred during any step */
    data class Error(
        val message: String,
        val exception: Exception?,
        val step: ErrorStep = ErrorStep.UNKNOWN
    ) : AuthState()
}

enum class ErrorStep {
    VALIDATION,
    KEY_GENERATION,
    REGISTRATION,
    AUTHENTICATION,
    UNKNOWN
}

/**
 * Authentication ViewModel - MVVM with MVI principles
 *
 * Manages user authentication flow:
 * 1. Validate phone number input
 * 2. Generate cryptographic identity (runs on Dispatchers.Default)
 * 3. Register with server
 * 4. Authenticate and establish session
 *
 * All heavy cryptographic work is pushed to background threads.
 * UI always observes a single StateFlow<AuthState>
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Initiate secure login flow
     *
     * @param phoneNumber User's phone number in E.164 format (e.g., "+1234567890")
     */
    fun secureLogin(phoneNumber: String) {
        viewModelScope.launch {
            try {
                // Step 1: Validate input
                val validationError = validatePhoneNumber(phoneNumber)
                if (validationError != null) {
                    _authState.value = AuthState.Error(
                        message = validationError,
                        exception = null,
                        step = ErrorStep.VALIDATION
                    )
                    return@launch
                }

                // Step 2: Generate cryptographic identity
                _authState.value = AuthState.GeneratingKeys(progress = 0)

                val generationResult = authRepository.generateIdentity(phoneNumber)

                when (generationResult) {
                    is IdentityGenerationResult.Success -> {
                        val identity = generationResult.identity

                        // Step 3: Register identity with server
                        _authState.value = AuthState.Uploading(identity)

                        val registerResult = authRepository.registerIdentity(identity)

                        when (registerResult) {
                            is AuthenticationResult.Success -> {
                                // Step 4: Authenticate
                                _authState.value = AuthState.Success(
                                    userId = registerResult.userId,
                                    identity = registerResult.identity
                                )
                            }

                            is AuthenticationResult.Error -> {
                                _authState.value = AuthState.Error(
                                    message = registerResult.message,
                                    exception = registerResult.exception,
                                    step = ErrorStep.REGISTRATION
                                )
                            }

                            AuthenticationResult.InvalidInput -> {
                                _authState.value = AuthState.Error(
                                    message = "Invalid identity format",
                                    exception = null,
                                    step = ErrorStep.VALIDATION
                                )
                            }

                            AuthenticationResult.NetworkError -> {
                                _authState.value = AuthState.Error(
                                    message = "Network error. Please check your connection.",
                                    exception = null,
                                    step = ErrorStep.REGISTRATION
                                )
                            }
                        }
                    }

                    is IdentityGenerationResult.Error -> {
                        _authState.value = AuthState.Error(
                            message = generationResult.message,
                            exception = generationResult.exception,
                            step = ErrorStep.KEY_GENERATION
                        )
                    }

                    IdentityGenerationResult.Cancelled -> {
                        _authState.value = AuthState.Idle
                    }
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    message = "Unexpected error: ${e.message}",
                    exception = e,
                    step = ErrorStep.UNKNOWN
                )
            }
        }
    }

    /**
     * Retry after error
     *
     * @param phoneNumber Phone number to retry with
     */
    fun retryLogin(phoneNumber: String) {
        _authState.value = AuthState.Idle
        secureLogin(phoneNumber)
    }

    /**
     * Cancel current operation
     */
    fun cancelLogin() {
        _authState.value = AuthState.Idle
    }

    /**
     * Validate phone number format
     *
     * @return Error message if invalid, null if valid
     */
    private fun validatePhoneNumber(phoneNumber: String): String? {
        return when {
            phoneNumber.isBlank() -> "Phone number cannot be empty"
            !phoneNumber.startsWith("+") -> "Phone number must start with +"
            phoneNumber.length < 10 -> "Phone number is too short"
            phoneNumber.length > 15 -> "Phone number is too long"
            else -> null
        }
    }
}

