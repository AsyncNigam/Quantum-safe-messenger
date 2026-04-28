package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.usecase.GetChatHistoryUseCase
import com.nigdroid.quantummessenger.domain.usecase.ReceiveMessageResult
import com.nigdroid.quantummessenger.domain.usecase.ReceiveMessageUseCase
import com.nigdroid.quantummessenger.domain.usecase.SendMessageUseCase
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
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
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)

    // Public immutable state for UI consumption
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Current user and recipient IDs
    private var currentUserId: String = ""
    private var recipientUserId: String = ""
    private var contactName: String? = null
    private var isContactSaved: Boolean = false

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

            // Load chat history and observe for changes
            // (Incoming messages are handled globally by HomeViewModel,
            //  which saves them to Room. This Flow auto-updates when Room changes.)
            loadChatHistory()
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
                        // Update UI with loaded messages
                        _uiState.update { currentState ->
                            ChatUiState.Success(
                                messages = messages,
                                isSending = (currentState as? ChatUiState.Success)?.isSending ?: false,
                                currentUserId = currentUserId,
                                contactName = contactName,
                                isContactSaved = isContactSaved
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
                            // Message was successfully saved to database
                            // The UI will automatically update via the chat history flow
                        }
                        is ReceiveMessageResult.Error -> {
                            // Handle error without crashing the app
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
                                kotlinx.coroutines.delay(3000) // Show error for 3 seconds
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
                    kotlinx.coroutines.delay(3000)
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
                // First, fetch the contact's public keys from the server (required)
                // For now, we'll create a placeholder
                val existingContact = contactDao.getContactById(recipientUserId)
                
                if (existingContact != null) {
                    // Contact already exists, just update the display name
                    val updated = existingContact.copy(displayName = displayName)
                    contactDao.insertContact(updated)
                    contactName = displayName
                    isContactSaved = true
                } else {
                    // Cannot save contact without its public keys
                    // This should be handled by AddContactUseCase in normal flow
                    _uiState.update { currentState ->
                        ChatUiState.Error(
                            message = "Cannot save contact — public keys not available. Please add via fingerprint first.",
                            messages = (currentState as? ChatUiState.Success)?.messages ?: emptyList()
                        )
                    }
                    return@launch
                }
                
                // Update UI
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
}
