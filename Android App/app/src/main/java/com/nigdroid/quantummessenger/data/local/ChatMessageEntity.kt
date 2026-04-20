package com.nigdroid.quantummessenger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a chat message stored in the encrypted database.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: String,
    val receiverId: String,
    val content: String, // Encrypted content
    val timestamp: Long,
    val messageType: LocalMessageType = LocalMessageType.TEXT,
    val isRead: Boolean = false
)

enum class LocalMessageType {
    TEXT,
    IMAGE,
    FILE
}
