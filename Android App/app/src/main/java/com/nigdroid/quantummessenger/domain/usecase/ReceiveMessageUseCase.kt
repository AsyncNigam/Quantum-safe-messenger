package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.data.repository.ChatRepositoryImpl
import com.nigdroid.quantummessenger.domain.model.ChatMessage as DomainChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.proto.ChatMessage as ProtoMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import java.security.MessageDigest
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ReceiveMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    private val contactDao: ContactDao
) {

    private val recentlyProcessed: MutableSet<String> = Collections.synchronizedSet(
        LinkedHashSet<String>(MAX_DEDUP_CACHE)
    )

    operator fun invoke(): Flow<ReceiveMessageResult> {
        return webSocketManager.incomingMessages
            .mapNotNull { protoMessage ->
                try {
                    val payloadString = String(protoMessage.payload.toByteArray(), Charsets.UTF_8)

                    // ── Block check: silently drop messages from blocked contacts ──
                    val senderContact = contactDao.getContactById(protoMessage.senderId)
                    if (senderContact?.isBlocked == true) {
                        // ACK the message so the server removes it, but don't save locally
                        val backendId = webSocketManager.getBackendMessageId(protoMessage)
                        webSocketManager.emitAck(backendId ?: "")
                        return@mapNotNull null
                    }

                    val messageUuid = generateDedupUuid(
                        protoMessage.senderId,
                        protoMessage.recipientId,
                        protoMessage.timestamp,
                        payloadString
                    )

                    if (recentlyProcessed.contains(messageUuid)) {
                        return@mapNotNull null
                    }

                    val repo = chatRepository as? ChatRepositoryImpl
                    if (repo?.existsByUuid(messageUuid) == true) {
                        recentlyProcessed.addBounded(messageUuid)
                        return@mapNotNull null
                    }

                    val domainMessage = DomainChatMessage(
                        senderId = protoMessage.senderId,
                        receiverId = protoMessage.recipientId,
                        content = payloadString,
                        timestamp = protoMessage.timestamp,
                        messageType = MessageType.TEXT,
                        isRead = false,
                        messageUuid = messageUuid
                    )

                    val insertedId = chatRepository.sendMessage(domainMessage)

                    if (insertedId == -1L) {
                        recentlyProcessed.addBounded(messageUuid)
                        return@mapNotNull null
                    }

                    recentlyProcessed.addBounded(messageUuid)

                    // Use backend-generated messageId for ACK if available,
                    // falling back to the local dedup UUID.
                    // The backend stores messages keyed by its own messageId,
                    // so we must echo that back for proper deletion.
                    val backendId = webSocketManager.getBackendMessageId(protoMessage)
                    webSocketManager.emitAck(backendId ?: messageUuid)

                    ReceiveMessageResult.Success(domainMessage)
                } catch (e: Exception) {
                    ReceiveMessageResult.Error("Failed to process incoming message: ${e.message}", e)
                }
            }
            .catch { e ->
                emit(ReceiveMessageResult.Error("WebSocket error: ${e.message}", e))
            }
    }

    private fun generateDedupUuid(
        senderId: String,
        recipientId: String,
        timestamp: Long,
        content: String
    ): String {
        val input = "$senderId|$recipientId|$timestamp|$content"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun MutableSet<String>.addBounded(element: String) {
        if (size >= MAX_DEDUP_CACHE) {
            val iterator = iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        add(element)
    }

    companion object {
        private const val MAX_DEDUP_CACHE = 500
    }
}

sealed class ReceiveMessageResult {
    data class Success(val message: DomainChatMessage) : ReceiveMessageResult()
    data class Error(val message: String, val exception: Throwable? = null) : ReceiveMessageResult()
}
