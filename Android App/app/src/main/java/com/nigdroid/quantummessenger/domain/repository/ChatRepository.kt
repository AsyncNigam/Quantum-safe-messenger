package com.nigdroid.quantummessenger.domain.repository

import com.nigdroid.quantummessenger.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(message: ChatMessage): Long
    fun getMessagesBetweenUsers(userId: String, otherUserId: String): Flow<List<ChatMessage>>
    fun getUnreadMessagesForUser(userId: String): Flow<List<ChatMessage>>
    suspend fun markMessageAsRead(messageId: Long)
    suspend fun deleteMessage(messageId: Long)
    suspend fun deleteConversation(userId: String, otherUserId: String)
    suspend fun updateMessageStatus(messageId: Long, status: com.nigdroid.quantummessenger.domain.model.MessageStatus)
}
