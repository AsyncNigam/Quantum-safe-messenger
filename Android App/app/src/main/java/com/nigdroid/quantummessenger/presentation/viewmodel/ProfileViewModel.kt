package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager
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
}
