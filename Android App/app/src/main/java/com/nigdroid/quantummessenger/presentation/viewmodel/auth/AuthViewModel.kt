package com.nigdroid.quantummessenger.presentation.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
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

sealed class AuthState {
    object Idle : AuthState()
    data class GeneratingKeys(val progress: Int = 0) : AuthState()
    data class Uploading(val identity: Identity) : AuthState()
    data class Success(val userId: String, val identity: Identity) : AuthState()
    data class Error(val message: String, val exception: Exception?, val step: ErrorStep = ErrorStep.UNKNOWN) : AuthState()
}

enum class ErrorStep {
    VALIDATION, KEY_GENERATION, REGISTRATION, AUTHENTICATION, UNKNOWN
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun secureLogin(phoneNumber: String) {
        viewModelScope.launch {
            try {
                val validationError = validatePhoneNumber(phoneNumber)
                if (validationError != null) {
                    _authState.value = AuthState.Error(validationError, null, ErrorStep.VALIDATION)
                    return@launch
                }

                _authState.value = AuthState.GeneratingKeys(progress = 0)
                val generationResult = authRepository.generateIdentity(phoneNumber)

                when (generationResult) {
                    is IdentityGenerationResult.Success -> {
                        val identity = generationResult.identity
                        _authState.value = AuthState.Uploading(identity)

                        val registerResult = authRepository.registerIdentity(identity)
                        when (registerResult) {
                            is AuthenticationResult.Success -> {
                                sessionManager.setUserRegistered(true)
                                _authState.value = AuthState.Success(registerResult.userId, registerResult.identity)
                            }
                            is AuthenticationResult.Error -> {
                                _authState.value = AuthState.Error(registerResult.message, registerResult.exception, ErrorStep.REGISTRATION)
                            }
                            AuthenticationResult.InvalidInput -> {
                                _authState.value = AuthState.Error("Invalid identity format", null, ErrorStep.VALIDATION)
                            }
                            AuthenticationResult.NetworkError -> {
                                _authState.value = AuthState.Error("Network error. Please check your connection.", null, ErrorStep.REGISTRATION)
                            }
                        }
                    }
                    is IdentityGenerationResult.Error -> {
                        _authState.value = AuthState.Error(generationResult.message, generationResult.exception, ErrorStep.KEY_GENERATION)
                    }
                    IdentityGenerationResult.Cancelled -> {
                        _authState.value = AuthState.Idle
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Unexpected error: ${e.message}", e, ErrorStep.UNKNOWN)
            }
        }
    }

    fun retryLogin(phoneNumber: String) {
        _authState.value = AuthState.Idle
        secureLogin(phoneNumber)
    }

    fun cancelLogin() {
        _authState.value = AuthState.Idle
    }

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
