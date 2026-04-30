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
import java.util.UUID
import javax.inject.Inject

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
            val messageUuid = UUID.randomUUID().toString().replace("-", "")

            val domainMessage = ChatMessage(
                senderId = senderId,
                receiverId = receiverId,
                content = plainTextContent,
                timestamp = System.currentTimeMillis(),
                messageType = messageType,
                status = MessageStatus.PENDING,
                messageUuid = messageUuid
            )

            val messageId = chatRepository.sendMessage(domainMessage)

            val protoMessage = ProtoMessage.newBuilder()
                .setSenderId(senderId)
                .setRecipientId(receiverId)
                .setPayload(ByteString.copyFrom(plainTextContent.toByteArray(Charsets.UTF_8)))
                .setTimestamp(domainMessage.timestamp)
                .build()

            try {
                if (webSocketManager.isConnected()) {
                    webSocketManager.sendMessage(protoMessage)
                    chatRepository.updateMessageStatus(messageId, MessageStatus.SENT)
                } else {
                    enqueueBackgroundSend(messageId, protoMessage, receiverId)
                }
            } catch (e: Exception) {
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
