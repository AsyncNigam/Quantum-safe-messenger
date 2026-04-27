package com.nigdroid.quantummessenger.presentation.viewmodel

import com.nigdroid.quantummessenger.domain.model.ChatMessage

/**
 * Sealed class representing all possible UI states for the Chat screen.
 * Follows Unidirectional Data Flow (UDF) principles.
 */
sealed class ChatUiState {
    /**
     * Initial loading state when the screen first loads.
     */
    data object Loading : ChatUiState()

    /**
     * Success state with the current list of messages.
     * @param messages The list of decrypted chat messages to display
     * @param isSending Whether a message is currently being sent
     */
    data class Success(
        val messages: List<ChatMessage> = emptyList(),
        val isSending: Boolean = false,
        val currentUserId: String = "",
        val contactName: String? = null
    ) : ChatUiState()

    /**
     * Error state when an exception occurs during message sending, receiving, or loading.
     * @param message The error message to display to the user
     * @param messages The list of messages that were successfully loaded before the error
     */
    data class Error(
        val message: String,
        val messages: List<ChatMessage> = emptyList()
    ) : ChatUiState()
}
