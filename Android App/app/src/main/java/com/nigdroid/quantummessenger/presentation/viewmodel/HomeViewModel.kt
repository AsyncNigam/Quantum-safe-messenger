package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.domain.model.InboxItem
import com.nigdroid.quantummessenger.domain.usecase.GetInboxUseCase
import com.nigdroid.quantummessenger.domain.usecase.ReceiveMessageResult
import com.nigdroid.quantummessenger.domain.usecase.ReceiveMessageUseCase
import com.nigdroid.quantummessenger.domain.usecase.SyncContactsUseCase
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.network.notification.NotificationSoundManager
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
    private val sessionManager: SessionManager,
    private val contactDao: ContactDao,
    private val webSocketManager: WebSocketManager,
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val notificationSoundManager: NotificationSoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** The real user fingerprint — loaded from SessionManager. */
    private var currentUserId: String? = null

    /** Current search query. */
    private val _searchQuery = MutableStateFlow("")

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

            observeIncomingMessages()

            if (!webSocketManager.isConnected()) {
                webSocketManager.connect(currentUserId!!)
            }

            // Combine inbox items, contacts, and search query into a single flow
            combine(
                getInboxUseCase(currentUserId!!),
                contactDao.getAllContacts(),
                _searchQuery
            ) { inboxItems, allContacts, query ->
                // Find contacts that have NO conversation yet
                val inboxUserIds = inboxItems.map { it.userId }.toSet()
                val contactsWithoutChat = allContacts.filter { it.userId !in inboxUserIds }

                // Apply search filter
                val filteredInbox = if (query.isBlank()) inboxItems
                else inboxItems.filter { item ->
                    (item.displayName?.contains(query, ignoreCase = true) == true) ||
                    item.userId.contains(query, ignoreCase = true)
                }
                val filteredContacts = if (query.isBlank()) contactsWithoutChat
                else contactsWithoutChat.filter { contact ->
                    (contact.displayName?.contains(query, ignoreCase = true) == true) ||
                    contact.userId.contains(query, ignoreCase = true)
                }
                val filteredAllContacts = if (query.isBlank()) allContacts
                else allContacts.filter { contact ->
                    (contact.displayName?.contains(query, ignoreCase = true) == true) ||
                    contact.userId.contains(query, ignoreCase = true)
                }

                HomeUiState.Success(
                    inboxItems = filteredInbox,
                    contacts = filteredContacts,
                    allContacts = filteredAllContacts,
                    searchQuery = query
                )
            }
            .catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
            .collect { state ->
                _uiState.value = state
            }
        }
    }

    fun syncContacts() {
        viewModelScope.launch {
            // Trigger sync which handles its own errors and updates the DB
            syncContactsUseCase()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteConversation(otherUserId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            chatMessageDao.deleteConversation(userId, otherUserId)
        }
    }

    /**
     * Observes incoming messages from WebSocket and saves them to Room DB.
     * This runs as long as the HomeViewModel is alive (i.e. user is in the app).
     * Messages saved to DB will auto-update the inbox via Room's Flow queries.
     */
    private fun observeIncomingMessages() {
        viewModelScope.launch {
            receiveMessageUseCase()
                .collect { result ->
                    if (result is ReceiveMessageResult.Success) {
                        notificationSoundManager.playNotification()
                    }
                }
        }
    }
}

