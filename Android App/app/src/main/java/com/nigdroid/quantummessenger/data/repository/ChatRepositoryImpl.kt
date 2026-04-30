package com.nigdroid.quantummessenger.data.repository

import com.nigdroid.quantummessenger.data.crypto.CryptoManager
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ChatMessageEntity
import com.nigdroid.quantummessenger.data.local.LocalMessageType
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageStatus
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class ChatRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val cryptoManager: CryptoManager
) : ChatRepository {

    override suspend fun sendMessage(message: ChatMessage): Long {
        val encryptedContent = cryptoManager.encrypt(
            message.content.toByteArray(Charsets.UTF_8)
        )
        val encodedContent = android.util.Base64.encodeToString(encryptedContent, android.util.Base64.NO_WRAP)

        val entity = ChatMessageEntity(
            id = message.id,
            messageUuid = message.messageUuid,
            senderId = message.senderId,
            receiverId = message.receiverId,
            content = encodedContent,
            timestamp = message.timestamp,
            messageType = message.messageType.toLocalMessageType(),
            isRead = message.isRead,
            status = message.status
        )

        return chatMessageDao.insertMessage(entity)
    }

    override fun getMessagesBetweenUsers(userId: String, otherUserId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesBetweenUsers(userId, otherUserId)
            .map { entities ->
                entities.map { entity -> decryptEntity(entity) }
            }
    }

    override fun getUnreadMessagesForUser(userId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getUnreadMessagesForUser(userId)
            .map { entities ->
                entities.map { entity -> decryptEntity(entity) }
            }
    }

    override suspend fun markMessageAsRead(messageId: Long) {
        chatMessageDao.markMessageAsRead(messageId)
    }

    override suspend fun deleteMessage(messageId: Long) {
        chatMessageDao.deleteMessage(messageId)
    }

    override suspend fun deleteConversation(userId: String, otherUserId: String) {
        chatMessageDao.deleteConversation(userId, otherUserId)
    }

    override suspend fun updateMessageStatus(messageId: Long, status: MessageStatus) {
        chatMessageDao.updateMessageStatus(messageId, status)
    }

    suspend fun existsByUuid(uuid: String): Boolean {
        return chatMessageDao.existsByUuid(uuid)
    }


    private suspend fun decryptEntity(entity: ChatMessageEntity): ChatMessage {
        val decryptedContent = try {
            // Primary path: Base64-encoded encrypted content (new format)
            val encryptedBytes = android.util.Base64.decode(entity.content, android.util.Base64.NO_WRAP)
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                // Fallback: Legacy ISO_8859_1 encoded content (old format)
                val encryptedBytes = entity.content.toByteArray(Charsets.ISO_8859_1)
                val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
                String(decryptedBytes, Charsets.UTF_8)
            } catch (e2: Exception) {
                // Final fallback: show a clean placeholder instead of encrypted garbage
                "\uD83D\uDD12 Message"
            }
        }

        return ChatMessage(
            id = entity.id,
            messageUuid = entity.messageUuid,
            senderId = entity.senderId,
            receiverId = entity.receiverId,
            content = decryptedContent,
            timestamp = entity.timestamp,
            messageType = entity.messageType.toMessageType(),
            isRead = entity.isRead,
            status = entity.status
        )
    }
}

private fun MessageType.toLocalMessageType(): LocalMessageType = when (this) {
    MessageType.TEXT -> LocalMessageType.TEXT
    MessageType.IMAGE -> LocalMessageType.IMAGE
    MessageType.FILE -> LocalMessageType.FILE
}

private fun LocalMessageType.toMessageType(): MessageType = when (this) {
    LocalMessageType.TEXT -> MessageType.TEXT
    LocalMessageType.IMAGE -> MessageType.IMAGE
    LocalMessageType.FILE -> MessageType.FILE
}
