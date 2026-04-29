package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.network.notification.NotificationSoundManager
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.usecase.AddContactUseCase
import com.nigdroid.quantummessenger.domain.usecase.GetChatHistoryUseCase
import com.nigdroid.quantummessenger.domain.usecase.ReceiveMessageResult
import com.nigdroid.quantummessenger.domain.usecase.ReceiveMessageUseCase
import com.nigdroid.quantummessenger.domain.usecase.SendMessageUseCase
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.network.SocketEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val sessionManager: SessionManager,
    private val contactDao: ContactDao,
    private val chatMessageDao: ChatMessageDao,
    private val webSocketManager: WebSocketManager,
    private val notificationSoundManager: NotificationSoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""
    private var recipientUserId: String = ""
    private var contactName: String? = null
    private var isContactSaved: Boolean = false
    private var isNotificationsMuted: Boolean = false

    fun initialize(userId: String, participantId: String) {
        recipientUserId = participantId

        viewModelScope.launch {
            currentUserId = sessionManager.textFingerprint.firstOrNull() ?: ""

            if (currentUserId.isBlank()) {
                _uiState.value = ChatUiState.Error("Not registered — please restart the app.")
                return@launch
            }

            val contact = contactDao.getContactById(recipientUserId)
            contactName = contact?.displayName
            isContactSaved = contact != null

            if (!webSocketManager.isConnected()) {
                webSocketManager.connect(currentUserId)
            }

            loadChatHistory()
            observeDeletedEvents()
        }
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                getChatHistoryUseCase(currentUserId, recipientUserId)
                    .catch { e ->
                        _uiState.update { currentState ->
                            ChatUiState.Error(
                                message = "Failed to load chat history: ${e.message}",
                                messages = if (currentState is ChatUiState.Success) currentState.messages else emptyList()
                            )
                        }
                    }
                    .collect { messages ->
                        _uiState.update { currentState ->
                            ChatUiState.Success(
                                messages = messages,
                                isSending = (currentState as? ChatUiState.Success)?.isSending ?: false,
                                currentUserId = currentUserId,
                                contactName = contactName,
                                isContactSaved = isContactSaved,
                                isNotificationsMuted = isNotificationsMuted
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    ChatUiState.Error("Unexpected error: ${e.message}")
                }
            }
        }
    }

    private fun observeDeletedEvents() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                if (event is SocketEvent.UserDeleted &&
                    event.fingerprint == recipientUserId) {
                    _uiState.update { current ->
                        when (current) {
                            is ChatUiState.Success -> current.copy(recipientDeleted = true)
                            else -> current
                        }
                    }
                }
            }
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            receiveMessageUseCase()
                .collect { result ->
                    when (result) {
                        is ReceiveMessageResult.Success -> {
                            if (!isNotificationsMuted) {
                                notificationSoundManager.playNotification()
                            }
                        }
                        is ReceiveMessageResult.Error -> {
                            _uiState.update { currentState ->
                                when (currentState) {
                                    is ChatUiState.Success -> {
                                        ChatUiState.Error(
                                            message = result.message,
                                            messages = currentState.messages
                                        )
                                    }
                                    else -> {
                                        ChatUiState.Error(message = result.message)
                                    }
                                }
                            }

                            val currentMessages = (_uiState.value as? ChatUiState.Success)?.messages
                            if (currentMessages != null && currentMessages.isNotEmpty()) {
                                delay(3000)
                                _uiState.update {
                                    ChatUiState.Success(messages = currentMessages)
                                }
                            }
                        }
                    }
                }
        }
    }

    fun sendMessage(plainTextContent: String) {
        if (plainTextContent.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    if (currentState is ChatUiState.Success) {
                        currentState.copy(isSending = true)
                    } else {
                        currentState
                    }
                }

                sendMessageUseCase(
                    plainTextContent = plainTextContent,
                    senderId = currentUserId,
                    receiverId = recipientUserId,
                    messageType = MessageType.TEXT
                )

                _uiState.update { currentState ->
                    if (currentState is ChatUiState.Success) {
                        currentState.copy(isSending = false)
                    } else {
                        currentState
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    when (currentState) {
                        is ChatUiState.Success -> currentState.copy(isSending = false)
                        else -> currentState
                    }
                }

                _uiState.update { currentState ->
                    when (currentState) {
                        is ChatUiState.Success -> {
                            ChatUiState.Error(
                                message = "Failed to send message: ${e.message}",
                                messages = currentState.messages
                            )
                        }
                        else -> {
                            ChatUiState.Error(message = "Failed to send message: ${e.message}")
                        }
                    }
                }

                val currentMessages = (_uiState.value as? ChatUiState.Success)?.messages
                if (currentMessages != null && currentMessages.isNotEmpty()) {
                    delay(3000)
                    _uiState.update {
                        ChatUiState.Success(messages = currentMessages)
                    }
                }
            }
        }
    }

    fun markMessageAsRead(messageId: Long) {
        viewModelScope.launch {
            try {
            } catch (e: Exception) {
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    when (currentState) {
                        is ChatUiState.Success -> {
                            ChatUiState.Error(
                                message = "Failed to delete message",
                                messages = currentState.messages
                            )
                        }
                        else -> currentState
                    }
                }
            }
        }
    }

    fun retryLoadingMessages() {
        _uiState.update { ChatUiState.Loading }
        loadChatHistory()
    }

    fun clearChat() {
        viewModelScope.launch {
            try {
                chatMessageDao.deleteConversation(currentUserId, recipientUserId)
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    ChatUiState.Error(
                        message = "Failed to clear chat: ${e.message}",
                        messages = (currentState as? ChatUiState.Success)?.messages ?: emptyList()
                    )
                }
            }
        }
    }

    fun saveContact(displayName: String? = null) {
        viewModelScope.launch {
            try {
                val result = addContactUseCase(recipientUserId, displayName)
                
                when (result) {
                    is AddContactUseCase.Result.Success, 
                    is AddContactUseCase.Result.AlreadyExists -> {
                        val contact = contactDao.getContactById(recipientUserId)
                        contactName = contact?.displayName
                        isContactSaved = true
                        
                        _uiState.update { currentState ->
                            if (currentState is ChatUiState.Success) {
                                currentState.copy(
                                    contactName = contactName,
                                    isContactSaved = true
                                )
                            } else {
                                currentState
                            }
                        }
                    }
                    is AddContactUseCase.Result.Error -> {
                        _uiState.update { currentState ->
                            ChatUiState.Error(
                                message = result.message,
                                messages = (currentState as? ChatUiState.Success)?.messages ?: emptyList()
                            )
                        }
                    }
                    AddContactUseCase.Result.SelfAdd -> {}
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    ChatUiState.Error(
                        message = "Failed to save contact: ${e.message}",
                        messages = (currentState as? ChatUiState.Success)?.messages ?: emptyList()
                    )
                }
            }
        }
    }

    fun renameContact(newName: String) {
        viewModelScope.launch {
            try {
                val existingContact = contactDao.getContactById(recipientUserId)
                
                if (existingContact == null) {
                    _uiState.update { currentState ->
                        ChatUiState.Error(
                            message = "Contact not found",
                            messages = (currentState as? ChatUiState.Success)?.messages ?: emptyList()
                        )
                    }
                    return@launch
                }

                val updated = existingContact.copy(displayName = newName.takeIf { it.isNotBlank() })
                contactDao.insertContact(updated)
                contactName = updated.displayName
                
                _uiState.update { currentState ->
                    if (currentState is ChatUiState.Success) {
                        currentState.copy(contactName = contactName)
                    } else {
                        currentState
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    ChatUiState.Error(
                        message = "Failed to rename contact: ${e.message}",
                        messages = (currentState as? ChatUiState.Success)?.messages ?: emptyList()
                    )
                }
            }
        }
    }

    fun toggleMuteNotifications() {
        isNotificationsMuted = !isNotificationsMuted
        notificationSoundManager.setMuted(isNotificationsMuted)
        
        _uiState.update { currentState ->
            if (currentState is ChatUiState.Success) {
                currentState.copy(isNotificationsMuted = isNotificationsMuted)
            } else {
                currentState
            }
        }
    }
}
