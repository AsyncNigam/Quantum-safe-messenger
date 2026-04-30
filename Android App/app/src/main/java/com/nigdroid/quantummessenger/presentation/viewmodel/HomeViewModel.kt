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
import com.nigdroid.quantummessenger.network.SocketEvent
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.network.fcm.FcmTokenManager
import com.nigdroid.quantummessenger.network.notification.NotificationSoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInboxUseCase: GetInboxUseCase,
    private val syncContactsUseCase: SyncContactsUseCase,
    private val chatMessageDao: ChatMessageDao,
    private val sessionManager: SessionManager,
    private val contactDao: ContactDao,
    private val webSocketManager: WebSocketManager,
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val notificationSoundManager: NotificationSoundManager,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private val _searchQuery = MutableStateFlow("")

    init {
        loadUserAndObserveInbox()
        syncContacts()
    }

    private fun loadUserAndObserveInbox() {
        viewModelScope.launch {
            currentUserId = sessionManager.textFingerprint.firstOrNull()

            if (currentUserId == null) {
                _uiState.value = HomeUiState.Error("Not registered — please restart the app.")
                return@launch
            }

            // Start collecting incoming messages FIRST
            observeIncomingMessages()

            // Connect WebSocket (does NOT auto-drain on server)
            if (!webSocketManager.isConnected()) {
                webSocketManager.connect(currentUserId!!)
            }

            // Wait for the socket to actually connect, then request drain.
            // The server only drains when the client emits 'request_drain',
            // ensuring the collector above is ready to process the messages.
            waitForConnectionAndDrain()

            syncFcmToken()

            combine(
                getInboxUseCase(currentUserId!!),
                contactDao.getAllContacts(),
                _searchQuery
            ) { inboxItems, allContacts, query ->
                val inboxUserIds = inboxItems.map { it.userId }.toSet()
                val contactsWithoutChat = allContacts.filter { it.userId !in inboxUserIds }

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

    private fun waitForConnectionAndDrain() {
        viewModelScope.launch {
            webSocketManager.events
                .filterIsInstance<SocketEvent.Connected>()
                .first()

            webSocketManager.requestDrain()
        }
    }

    private fun syncFcmToken() {
        viewModelScope.launch {
            try {
                fcmTokenManager.syncToken()
            } catch (_: Exception) {}
        }
    }
}
