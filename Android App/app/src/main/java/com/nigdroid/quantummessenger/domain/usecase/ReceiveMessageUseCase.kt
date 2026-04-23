package com.nigdroid.quantummessenger.domain.usecase

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
import javax.inject.Inject

/**
 * Use case for receiving and processing incoming messages.
 */
class ReceiveMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager
) {

    operator fun invoke(): Flow<ReceiveMessageResult> {
        return webSocketManager.events
            .filterIsInstance<SocketEvent.MessageReceived>()
            .mapNotNull { event ->
                try {
                    val protoMessage = event.message

                    // protoMessage is of type com.nigdroid.quantummessenger.proto.ChatMessage
                    // The getter names from Protobuf Java Lite are used here.
                    val payloadString = String(protoMessage.payload.toByteArray(), Charsets.UTF_8)

                    val domainMessage = DomainChatMessage(
                        senderId = protoMessage.senderId,
                        receiverId = protoMessage.recipientId,
                        content = payloadString,
                        timestamp = protoMessage.timestamp,
                        messageType = MessageType.TEXT,
                        isRead = false
                    )

                    chatRepository.sendMessage(domainMessage)
                    ReceiveMessageResult.Success(domainMessage)
                } catch (e: Exception) {
                    ReceiveMessageResult.Error("Failed to process incoming message: ${e.message}", e)
                }
            }
            .catch { e ->
                emit(ReceiveMessageResult.Error("WebSocket error: ${e.message}", e))
            }
    }
}

sealed class ReceiveMessageResult {
    data class Success(val message: DomainChatMessage) : ReceiveMessageResult()
    data class Error(val message: String, val exception: Throwable? = null) : ReceiveMessageResult()
}
