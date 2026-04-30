package com.nigdroid.quantummessenger.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.nigdroid.quantummessenger.domain.model.MessageStatus

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["messageUuid"], unique = true)]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageUuid: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val messageType: LocalMessageType = LocalMessageType.TEXT,
    val isRead: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)

enum class LocalMessageType {
    TEXT,
    IMAGE,
    FILE
}
