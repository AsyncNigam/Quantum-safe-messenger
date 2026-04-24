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
    object Authenticating : AuthState()
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

    fun signInWithGoogle(idToken: String, email: String, nonce: String? = null) {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating
            val result = authRepository.signInWithGoogle(idToken, nonce)
            
            if (result.isSuccess) {
                handleSuccessfulAuth(email)
            } else {
                _authState.value = AuthState.Error(
                    "Google Sign-In failed: ${result.exceptionOrNull()?.message}",
                    result.exceptionOrNull() as? Exception,
                    ErrorStep.AUTHENTICATION
                )
            }
        }
    }

    private suspend fun handleSuccessfulAuth(identifier: String) {
        try {
            _authState.value = AuthState.GeneratingKeys(progress = 0)
            val generationResult = authRepository.generateIdentity(identifier)

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
                        else -> {
                            _authState.value = AuthState.Error("Registration failed", null, ErrorStep.REGISTRATION)
                        }
                    }
                }
                is IdentityGenerationResult.Error -> {
                    _authState.value = AuthState.Error(generationResult.message, generationResult.exception, ErrorStep.KEY_GENERATION)
                }
                else -> _authState.value = AuthState.Idle
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Unexpected error: ${e.message}", e, ErrorStep.UNKNOWN)
        }
    }

    fun retryLogin() {
        _authState.value = AuthState.Idle
    }
}
