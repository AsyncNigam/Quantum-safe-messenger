package com.nigdroid.quantummessenger.presentation.viewmodel

import com.nigdroid.quantummessenger.domain.model.ChatMessage

sealed class ChatUiState {
    data object Loading : ChatUiState()

    data class Success(
        val messages: List<ChatMessage> = emptyList(),
        val isSending: Boolean = false,
        val currentUserId: String = "",
        val contactName: String? = null,
        val isContactSaved: Boolean = false,
        val recipientDeleted: Boolean = false,
        val isNotificationsMuted: Boolean = false
    ) : ChatUiState()

    data class Error(
        val message: String,
        val messages: List<ChatMessage> = emptyList()
    ) : ChatUiState()
}
