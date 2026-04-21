package com.nigdroid.quantummessenger.domain.usecase

import androidx.work.*
import com.google.protobuf.ByteString
import com.nigdroid.quantummessenger.data.worker.SendMessageWorker
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageStatus
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.proto.ChatMessage as ProtoMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Enhanced Use Case for sending messages with Background Resilience.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    private val workManager: WorkManager
) {

    suspend operator fun invoke(
        plainTextContent: String,
        senderId: String,
        receiverId: String,
        messageType: MessageType = MessageType.TEXT
    ) {
        withContext(Dispatchers.IO) {
            // 1. Prepare domain model
            val domainMessage = ChatMessage(
                senderId = senderId,
                receiverId = receiverId,
                content = plainTextContent,
                timestamp = System.currentTimeMillis(),
                messageType = messageType,
                status = MessageStatus.PENDING // Initially pending
            )

            // 2. Save to local DB and get the generated ID
            val messageId = chatRepository.sendMessage(domainMessage)

            // 3. Prepare Protobuf payload
            val protoMessage = ProtoMessage.newBuilder()
                .setSenderId(senderId)
                .setRecipientId(receiverId)
                .setPayload(ByteString.copyFrom(plainTextContent.toByteArray(Charsets.UTF_8)))
                .setTimestamp(domainMessage.timestamp)
                .build()

            try {
                // 4. Try immediate send via WebSocket
                if (webSocketManager.isConnected()) {
                    webSocketManager.sendMessage(protoMessage)
                    // Update status to SENT immediately
                    chatRepository.updateMessageStatus(messageId, MessageStatus.SENT)
                } else {
                    // Fallback to WorkManager if not connected
                    enqueueBackgroundSend(messageId, protoMessage, receiverId)
                }
            } catch (e: Exception) {
                // If immediate send fails, enqueue for background retry
                enqueueBackgroundSend(messageId, protoMessage, receiverId)
            }
        }
    }

    private fun enqueueBackgroundSend(
        messageId: Long,
        protoMessage: ProtoMessage,
        recipientId: String
    ) {
        val inputData = Data.Builder()
            .putLong(SendMessageWorker.KEY_MESSAGE_ID, messageId)
            .putByteArray(SendMessageWorker.KEY_PAYLOAD, protoMessage.toByteArray())
            .putString(SendMessageWorker.KEY_RECIPIENT_ID, recipientId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sendRequest = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "send_msg_$messageId",
            ExistingWorkPolicy.REPLACE,
            sendRequest
        )
    }
}
