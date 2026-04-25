package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val sessionManager: SessionManager
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)

    // Public immutable state for UI consumption
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Current user and recipient IDs
    private var currentUserId: String = ""
    private var recipientUserId: String = ""

    /**
     * Initializes the ViewModel with the recipient and starts observing messages.
     * The current user's ID is loaded from SessionManager (real fingerprint).
     *
     * @param userId The ID of the current user (optional — auto-loaded from session)
     * @param participantId The ID of the user to chat with
     */
    fun initialize(userId: String, participantId: String) {
        recipientUserId = participantId

        viewModelScope.launch {
            // Use provided userId, or fall back to SessionManager fingerprint
            currentUserId = userId.ifBlank {
                sessionManager.textFingerprint.firstOrNull() ?: ""
            }

            if (currentUserId.isBlank()) {
                _uiState.value = ChatUiState.Error("Not registered — please restart the app.")
                return@launch
            }

            // Load chat history and observe for changes
            loadChatHistory()

            // Observe incoming messages from WebSocket
            observeIncomingMessages()
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
                            if (currentState is ChatUiState.Loading) {
                                ChatUiState.Success(messages = messages)
                            } else {
                                ChatUiState.Success(
                                    messages = messages,
                                    isSending = (currentState as? ChatUiState.Success)?.isSending ?: false
                                )
                            }
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
}
