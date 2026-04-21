package com.nigdroid.quantummessenger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chat messages.
 */
@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE (senderId = :userId AND receiverId = :otherUserId) OR (senderId = :otherUserId AND receiverId = :userId) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsers(userId: String, otherUserId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadMessagesForUser(userId: String): Flow<List<ChatMessageEntity>>

    @Query("UPDATE chat_messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markMessageAsRead(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE (senderId = :userId AND receiverId = :otherUserId) OR (senderId = :otherUserId AND receiverId = :userId)")
    suspend fun deleteConversation(userId: String, otherUserId: String)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: com.nigdroid.quantummessenger.domain.model.MessageStatus)

    @Query("""
        SELECT * FROM chat_messages 
        WHERE (senderId = :userId OR receiverId = :userId)
        AND id IN (
            SELECT MAX(id) FROM chat_messages 
            GROUP BY CASE WHEN senderId = :userId THEN receiverId ELSE senderId END
        )
        ORDER BY timestamp DESC
    """)
    fun getLastMessagesForUser(userId: String): Flow<List<ChatMessageEntity>>
}
