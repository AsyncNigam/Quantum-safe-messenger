package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.network.notification.NotificationSoundManager
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageType
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

/**
 * ViewModel for the Chat screen.
 *
 * Manages the application state following Unidirectional Data Flow (UDF) principles:
 * - Exposes a single StateFlow<ChatUiState> for the entire screen state
 * - Handles user actions (sendMessage)
 * - Observes incoming messages
 * - Loads chat history
 * - Manages errors gracefully
 *
 * All coroutines are launched in viewModelScope to ensure proper cleanup.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val receiveMessageUseCase: ReceiveMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
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

    /**
     * Initializes the ViewModel with the recipient and starts observing messages.
     * The current user's ID is ALWAYS loaded from SessionManager (real fingerprint).
     *
     * @param userId Ignored — kept for API compat but SessionManager is the source of truth
     * @param participantId The ID of the user to chat with
     */
    fun initialize(userId: String, participantId: String) {
        recipientUserId = participantId

        viewModelScope.launch {
            // ALWAYS load from SessionManager — never trust the parameter
            currentUserId = sessionManager.textFingerprint.firstOrNull() ?: ""

            if (currentUserId.isBlank()) {
                _uiState.value = ChatUiState.Error("Not registered — please restart the app.")
                return@launch
            }

            // Look up contact display name and check if saved
            val contact = contactDao.getContactById(recipientUserId)
            contactName = contact?.displayName
            isContactSaved = contact != null

            // Connect WebSocket with our fingerprint for auth
            if (!webSocketManager.isConnected()) {
                webSocketManager.connect(currentUserId)
            }

            loadChatHistory()
            observeDeletedEvents()
        }
    }


    /**
     * Loads the chat history between the current user and the recipient.
     * Updates the UI state with Success or Error depending on the outcome.
     */
    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                getChatHistoryUseCase(currentUserId, recipientUserId)
                    .catch { e ->
                        // Handle unexpected errors loading messages
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

    /**
     * Observes incoming messages from the WebSocket and saves them to the database.
     * Handles both successful receptions and errors gracefully.
     */
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

                            // After a brief delay, return to success state if messages exist
                            val currentMessages = (_uiState.value as? ChatUiState.Success)?.messages
                            if (currentMessages != null && currentMessages.isNotEmpty()) {
                                delay(3000) // Show error for 3 seconds
                                _uiState.update {
                                    ChatUiState.Success(messages = currentMessages)
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Sends a text message to the recipient.
     * Updates the UI state to show sending indicator while in flight.
     *
     * @param plainTextContent The plain text message to send
     */
    fun sendMessage(plainTextContent: String) {
        if (plainTextContent.isBlank()) {
            return // Ignore empty messages
        }

        viewModelScope.launch {
            try {
                // Update UI to show sending indicator
                _uiState.update { currentState ->
                    if (currentState is ChatUiState.Success) {
                        currentState.copy(isSending = true)
                    } else {
                        currentState
                    }
                }

                // Execute the use case to send message
                sendMessageUseCase(
                    plainTextContent = plainTextContent,
                    senderId = currentUserId,
                    receiverId = recipientUserId,
                    messageType = MessageType.TEXT
                )

                // Update UI to hide sending indicator
                _uiState.update { currentState ->
                    if (currentState is ChatUiState.Success) {
                        currentState.copy(isSending = false)
                    } else {
                        currentState
                    }
                }
            } catch (e: Exception) {
                // Handle send error gracefully
                _uiState.update { currentState ->
                    when (currentState) {
                        is ChatUiState.Success -> {
                            currentState.copy(isSending = false)
                        }
                        else -> currentState
                    }
                }

                // Emit error but keep existing messages
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

                // After a brief delay, return to success state if messages exist
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

    /**
     * Marks a message as read.
     *
     * @param messageId The ID of the message to mark as read
     */
    fun markMessageAsRead(messageId: Long) {
        viewModelScope.launch {
            try {
                // Use repository to mark as read (called directly via use case if needed)
                // For now, this is a placeholder
            } catch (e: Exception) {
                // Handle error silently for read receipt failures
            }
        }
    }

    /**
     * Deletes a message from the conversation.
     *
     * @param messageId The ID of the message to delete
     */
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                // Use repository to delete message
                // Implementation would depend on repository interface
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

    /**
     * Retries loading chat history after an error.
     */
    fun retryLoadingMessages() {
        _uiState.update { ChatUiState.Loading }
        loadChatHistory()
    }

    /**
     * Clears all messages in the current conversation.
     * The contact remains — only chat history is deleted.
     */
    fun clearChat() {
        viewModelScope.launch {
            try {
                chatMessageDao.deleteConversation(currentUserId, recipientUserId)
                // Room Flow will automatically emit an empty list
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

    /**
     * Saves the current contact to the database.
     * Uses the recipientUserId (fingerprint) and current contactName.
     */
    fun saveContact(displayName: String? = null) {
        viewModelScope.launch {
            try {
                val existingContact = contactDao.getContactById(recipientUserId)
                
                if (existingContact != null) {
                    val updated = existingContact.copy(displayName = displayName)
                    contactDao.insertContact(updated)
                    contactName = displayName
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
                } else {
                    _uiState.update { currentState ->
                        ChatUiState.Error(
                            message = "Contact not found",
                            messages = (currentState as? ChatUiState.Success)?.messages ?: emptyList()
                        )
                    }
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

    /**
     * Renames an existing saved contact.
     *
     * @param newName The new display name for the contact
     */
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
                
                // Update UI
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
