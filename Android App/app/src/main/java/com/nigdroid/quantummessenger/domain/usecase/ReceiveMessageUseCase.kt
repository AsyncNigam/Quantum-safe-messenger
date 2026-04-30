package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.data.repository.ChatRepositoryImpl
import com.nigdroid.quantummessenger.domain.model.ChatMessage as DomainChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import com.nigdroid.quantummessenger.network.SocketEvent
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.proto.ChatMessage as ProtoMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import java.security.MessageDigest
import java.util.Collections
import javax.inject.Inject


class ReceiveMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager
) {

    /**
     * In-memory set of recently processed message UUIDs to provide fast dedup
     * before hitting the database. Bounded to prevent memory leaks.
     */
    private val recentlyProcessed: MutableSet<String> = Collections.synchronizedSet(
        LinkedHashSet<String>(MAX_DEDUP_CACHE)
    )

    operator fun invoke(): Flow<ReceiveMessageResult> {
        return webSocketManager.events
            .filterIsInstance<SocketEvent.MessageReceived>()
            .mapNotNull { event ->
                try {
                    val protoMessage = event.message

                    val payloadString = String(protoMessage.payload.toByteArray(), Charsets.UTF_8)

                    // Generate a deterministic UUID from message content for deduplication.
                    // Same sender + same timestamp + same content hash = same UUID.
                    val messageUuid = generateDedupUuid(
                        protoMessage.senderId,
                        protoMessage.recipientId,
                        protoMessage.timestamp,
                        payloadString
                    )

                    // Fast in-memory dedup check
                    if (recentlyProcessed.contains(messageUuid)) {
                        return@mapNotNull null
                    }

                    // Database-level dedup check
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

                    // insertMessage with IGNORE returns -1 if the row was skipped (duplicate)
                    if (insertedId == -1L) {
                        recentlyProcessed.addBounded(messageUuid)
                        return@mapNotNull null
                    }

                    recentlyProcessed.addBounded(messageUuid)

                    // Emit ACK back to server so it knows we processed this message
                    webSocketManager.emitAck(messageUuid)

                    ReceiveMessageResult.Success(domainMessage)
                } catch (e: Exception) {
                    ReceiveMessageResult.Error("Failed to process incoming message: ${e.message}", e)
                }
            }
            .catch { e ->
                emit(ReceiveMessageResult.Error("WebSocket error: ${e.message}", e))
            }
    }

    /**
     * Generates a deterministic UUID from message attributes for deduplication.
     * Uses SHA-256 hash of sender + recipient + timestamp + content to produce
     * a consistent identifier that survives WebSocket reconnections and offline queue drains.
     */
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

    /**
     * Adds to the set while maintaining a bounded size to prevent memory leaks.
     */
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
