package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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

    /** Placeholder keys — in production, read from Keystore */
    private val _mlKemPublicKey = MutableStateFlow("(stored in Android Keystore)")
    val mlKemPublicKey: StateFlow<String> = _mlKemPublicKey.asStateFlow()

    private val _x25519PublicKey = MutableStateFlow("(stored in Android Keystore)")
    val x25519PublicKey: StateFlow<String> = _x25519PublicKey.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

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
    }

    fun saveDisplayName(name: String) {
        viewModelScope.launch {
            _isSaving.value = true
            sessionManager.setDisplayName(name.ifBlank { null })
            _isSaving.value = false
        }
    }
}
