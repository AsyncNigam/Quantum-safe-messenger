package com.nigdroid.quantummessenger.domain.model

data class ChatMessage(
    val id: Long = 0,
    val messageUuid: String = "",
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val messageType: MessageType = MessageType.TEXT,
    val isRead: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageType {
    TEXT,
    IMAGE,
    FILE
}
