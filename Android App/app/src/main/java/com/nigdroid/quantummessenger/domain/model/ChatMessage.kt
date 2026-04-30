package com.nigdroid.quantummessenger.domain.model

/**
 * Domain model for a chat message.
 * This represents the business logic entity, independent of data storage or UI concerns.
 */
data class ChatMessage(
    val id: Long = 0,
    val messageUuid: String = "",
    val senderId: String,
    val receiverId: String,
    val content: String, // Plain text content (decrypted)
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
