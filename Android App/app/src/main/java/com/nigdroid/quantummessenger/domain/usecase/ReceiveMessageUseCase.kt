package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import com.nigdroid.quantummessenger.network.SocketEvent
import com.nigdroid.quantummessenger.network.WebSocketManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

/**
 * Use case for receiving and processing incoming messages.
 *
 * This use case:
 * 1. Listens to the WebSocketManager's incoming message flow
 * 2. Decrypts the Protobuf payload via the ChatRepository
 * 3. Saves the decrypted message to the local Room Database
 * 4. Emits errors gracefully for UI handling
 *
 * Errors during decryption or database insertion are caught and emitted
 * so the app never crashes due to protocol issues.
 */
class ReceiveMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager
) {

    /**
     * Observes incoming messages from the server and saves them to the local database.
     *
     * This flow:
     * - Filters for MessageReceived events from the WebSocket
     * - Converts the Protobuf message to domain model
     * - Saves to local encrypted database
     * - Catches exceptions gracefully and emits ReceiveMessageResult.Error
     *
     * @return A Flow of ReceiveMessageResult (Success or Error)
     */
    operator fun invoke(): Flow<ReceiveMessageResult> {
        return webSocketManager.events
            .filterIsInstance<SocketEvent.MessageReceived>()
            .mapNotNull { event ->
                try {
                    val protoMessage = event.message

                    // Convert Protobuf to domain model
                    // Decode the payload from bytes to string
                    val payloadString = String(protoMessage.payload.toByteArray(), Charsets.UTF_8)

                    val domainMessage = ChatMessage(
                        senderId = protoMessage.senderId,
                        receiverId = protoMessage.recipientId,
                        content = payloadString, // Decrypted/decoded payload
                        timestamp = protoMessage.timestamp,
                        messageType = MessageType.TEXT, // Default, could be extended in proto
                        isRead = false
                    )

                    // Save to local encrypted database
                    chatRepository.sendMessage(domainMessage)

                    ReceiveMessageResult.Success(domainMessage)
                } catch (e: Exception) {
                    ReceiveMessageResult.Error("Failed to process incoming message: ${e.message}", e)
                }
            }
            .catch { e ->
                // Catch flow-level exceptions and emit as error result
                emit(ReceiveMessageResult.Error("WebSocket error: ${e.message}", e))
            }
    }
}

/**
 * Result sealed class for receive message operations.
 */
sealed class ReceiveMessageResult {
    /**
     * Successfully received and saved a message.
     * @param message The received and decrypted message
     */
    data class Success(val message: ChatMessage) : ReceiveMessageResult()

    /**
     * Error receiving or processing a message.
     * @param message Error description
     * @param exception The underlying exception
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ReceiveMessageResult()
}
