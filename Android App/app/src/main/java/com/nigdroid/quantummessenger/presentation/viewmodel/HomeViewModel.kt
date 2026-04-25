package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.domain.usecase.GetInboxUseCase
import com.nigdroid.quantummessenger.domain.usecase.SyncContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home/Inbox screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInboxUseCase: GetInboxUseCase,
    private val syncContactsUseCase: SyncContactsUseCase,
    private val chatMessageDao: ChatMessageDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** The real user fingerprint — loaded from SessionManager. */
    private var currentUserId: String? = null

    init {
        loadUserAndObserveInbox()
        syncContacts()
    }

    private fun loadUserAndObserveInbox() {
        viewModelScope.launch {
            // Get the user's fingerprint first, then observe inbox
            currentUserId = sessionManager.textFingerprint.firstOrNull()

            if (currentUserId == null) {
                _uiState.value = HomeUiState.Error("Not registered — please restart the app.")
                return@launch
            }

            getInboxUseCase(currentUserId!!)
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                }
                .collect { items ->
                    _uiState.value = HomeUiState.Success(items)
                }
        }
    }

    fun syncContacts() {
        viewModelScope.launch {
            // Trigger sync which handles its own errors and updates the DB
            syncContactsUseCase()
        }
    }

    fun deleteConversation(otherUserId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            chatMessageDao.deleteConversation(userId, otherUserId)
        }
    }
}
