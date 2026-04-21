package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetInboxUseCase @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val contactDao: ContactDao
) {
    // For this implementation, we'll simplify by combining flows from both DAOs.
    // In a production app, a more complex SQL join or a dedicated View might be used.
    operator fun invoke(currentUserId: String): Flow<List<InboxItem>> {
        return combine(
            chatMessageDao.getUnreadMessagesForUser(currentUserId),
            contactDao.getAllContacts()
        ) { unreadMessages, contacts ->
            // This is a simplified version. Realistically, we'd query for the latest message per contact.
            // For now, let's map contacts to InboxItems if they have any interaction.
            contacts.map { contact ->
                val unreadCount = unreadMessages.count { it.senderId == contact.userId }
                
                // Note: In a real app, we would fetch the actual last message from the DB.
                // Here we're using a placeholder as the messages flow isn't fully implemented for all.
                InboxItem(
                    userId = contact.userId,
                    displayName = contact.displayName ?: contact.phoneNumber,
                    lastMessage = "Tap to chat", // Placeholder
                    timestamp = System.currentTimeMillis(),
                    unreadCount = unreadCount
                )
            }
        }
    }
}
