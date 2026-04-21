package com.nigdroid.quantummessenger.domain.usecase

import com.google.protobuf.ByteString
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.proto.ChatMessageProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for sending a message.
 *
 * This use case:
 * 1. Takes a plain text message string
 * 2. Encrypts and stores it locally via the ChatRepository
 * 3. Serializes it into the Protobuf ChatMessage schema
 * 4. Sends it to the server via WebSocketManager
 *
 * All operations are performed on IO dispatcher to avoid blocking the main thread.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager
) {

    /**
     * Sends a message with the given parameters.
     *
     * @param plainTextContent The unencrypted message text
     * @param senderId The ID of the current user (sender)
     * @param receiverId The ID of the recipient user
     * @param messageType The type of message (TEXT, IMAGE, FILE)
     * @throws Exception if encryption, database insertion, or WebSocket transmission fails
     */
    suspend operator fun invoke(
        plainTextContent: String,
        senderId: String,
        receiverId: String,
        messageType: MessageType = MessageType.TEXT
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Create domain model with plain text
                val domainMessage = ChatMessage(
                    senderId = senderId,
                    receiverId = receiverId,
                    content = plainTextContent,
                    timestamp = System.currentTimeMillis(),
                    messageType = messageType,
                    isRead = false
                )

                // Save encrypted to local database
                val messageId = chatRepository.sendMessage(domainMessage)

                // Convert to Protobuf for transmission
                // Payload contains the plain text (server will handle encryption if needed)
                val protoMessage = ChatMessageProto.ChatMessage.newBuilder()
                    .setSenderId(senderId)
                    .setRecipientId(receiverId)
                    .setPayload(ByteString.copyFrom(plainTextContent.toByteArray(Charsets.UTF_8)))
                    .setTimestamp(System.currentTimeMillis())
                    .build()

                // Send via WebSocket
                webSocketManager.sendMessage(protoMessage)
            } catch (e: Exception) {
                // Re-throw to be handled by ViewModel with proper error messaging
                throw SendMessageException("Failed to send message: ${e.message}", e)
            }
        }
    }
}

/**
 * Custom exception for send message failures.
 */
class SendMessageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
