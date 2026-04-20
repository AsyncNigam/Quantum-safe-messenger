package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving chat history between the current user and another user.
 *
 * This use case exposes a Kotlin Flow that observes changes to the chat messages
 * in real-time from the local encrypted database. The messages are already decrypted
 * by the repository layer.
 */
class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    /**
     * Retrieves a flow of decrypted messages between two users.
     * The flow will emit updated lists whenever new messages are received and saved.
     *
     * @param userId The ID of the current user
     * @param otherUserId The ID of the other user in the conversation
     * @return A Flow that emits lists of decrypted ChatMessage objects
     */
    operator fun invoke(userId: String, otherUserId: String): Flow<List<ChatMessage>> {
        return chatRepository.getMessagesBetweenUsers(userId, otherUserId)
    }
}
