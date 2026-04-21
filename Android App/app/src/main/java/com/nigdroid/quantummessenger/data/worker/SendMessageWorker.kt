package com.nigdroid.quantummessenger.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.domain.model.MessageStatus
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.network.api.MessageApiService
import com.nigdroid.quantummessenger.proto.ChatMessage as ProtoMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Background worker to retry sending messages that failed to send immediately.
 */
@HiltWorker
class SendMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val webSocketManager: WebSocketManager,
    private val messageApiService: MessageApiService,
    private val chatMessageDao: ChatMessageDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val messageId = inputData.getLong(KEY_MESSAGE_ID, -1)
        val payload = inputData.getByteArray(KEY_PAYLOAD)
        val recipientId = inputData.getString(KEY_RECIPIENT_ID)

        if (messageId == -1L || payload == null || recipientId == null) {
            return@withContext Result.failure()
        }

        try {
            // Priority 1: WebSocket if currently connected
            if (webSocketManager.isConnected()) {
                val protoMessage = ProtoMessage.parseFrom(payload)
                webSocketManager.sendMessage(protoMessage)
                chatMessageDao.updateMessageStatus(messageId, MessageStatus.SENT)
                return@withContext Result.success()
            }

            // Priority 2: Fallback REST endpoint
            val requestBody = payload.toRequestBody("application/x-protobuf".toMediaType())
            val response = messageApiService.sendMessage(requestBody)

            if (response.isSuccessful) {
                chatMessageDao.updateMessageStatus(messageId, MessageStatus.SENT)
                Result.success()
            } else {
                // Server error, retry later
                Result.retry()
            }
        } catch (e: Exception) {
            // Network or parsing error
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                chatMessageDao.updateMessageStatus(messageId, MessageStatus.ERROR)
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        const val KEY_PAYLOAD = "payload"
        const val KEY_RECIPIENT_ID = "recipient_id"
    }
}
