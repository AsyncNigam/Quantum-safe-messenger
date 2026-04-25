package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to retrieve the list of inbox items (recent conversations).
 */
class GetInboxUseCase @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val contactDao: ContactDao
) {
    /**
     * Returns a flow of inbox items for the given user, combining message and contact data.
     */
    operator fun invoke(currentUserId: String): Flow<List<InboxItem>> {
        return combine(
            chatMessageDao.getLastMessagesForUser(currentUserId),
            chatMessageDao.getUnreadMessagesForUser(currentUserId),
            contactDao.getAllContacts()
        ) { lastMessages, unreadMessages, contacts ->
            lastMessages.map { lastMsg ->
                val otherUserId = if (lastMsg.senderId == currentUserId) lastMsg.receiverId else lastMsg.senderId
                val contact = contacts.find { it.userId == otherUserId }
                
                val unreadCount = unreadMessages.count { it.senderId == otherUserId }
                
                InboxItem(
                    userId = otherUserId,
                    displayName = contact?.displayName ?: otherUserId.take(8) + "…",
                    lastMessage = lastMsg.content,
                    timestamp = lastMsg.timestamp,
                    unreadCount = unreadCount,
                    avatarUrl = null // Can be expanded later
                )
            }
        }
    }
}
