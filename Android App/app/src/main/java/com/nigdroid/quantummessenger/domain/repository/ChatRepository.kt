package com.nigdroid.quantummessenger.domain.repository

import com.nigdroid.quantummessenger.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat message operations.
 * Defines the contract for data access in the domain layer.
 */
interface ChatRepository {

    /**
     * Sends a message by encrypting and storing it.
     * @param message The message to send (with plain text content)
     * @return The ID of the inserted message
     */
    suspend fun sendMessage(message: ChatMessage): Long

    /**
     * Retrieves messages between two users.
     * @param userId Current user ID
     * @param otherUserId Other user ID
     * @return Flow of decrypted messages
     */
    fun getMessagesBetweenUsers(userId: String, otherUserId: String): Flow<List<ChatMessage>>

    /**
     * Retrieves unread messages for a user.
     * @param userId User ID
     * @return Flow of decrypted unread messages
     */
    fun getUnreadMessagesForUser(userId: String): Flow<List<ChatMessage>>

    /**
     * Marks a message as read.
     * @param messageId Message ID
     */
    suspend fun markMessageAsRead(messageId: Long)

    /**
     * Deletes a message.
     * @param messageId Message ID
     */
    suspend fun deleteMessage(messageId: Long)

    /**
     * Deletes an entire conversation between two users.
     * @param userId Current user ID
     * @param otherUserId Other user ID
     */
    suspend fun deleteConversation(userId: String, otherUserId: String)
}
