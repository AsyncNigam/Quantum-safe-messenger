package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(userId: String, otherUserId: String): Flow<List<ChatMessage>> {
        return chatRepository.getMessagesBetweenUsers(userId, otherUserId)
    }
}
