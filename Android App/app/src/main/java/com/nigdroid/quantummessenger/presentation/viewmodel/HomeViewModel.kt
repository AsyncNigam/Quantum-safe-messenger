package com.nigdroid.quantummessenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.domain.model.InboxItem
import com.nigdroid.quantummessenger.domain.usecase.GetInboxUseCase
import com.nigdroid.quantummessenger.domain.usecase.SyncContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInboxUseCase: GetInboxUseCase,
    private val syncContactsUseCase: SyncContactsUseCase,
    private val chatMessageDao: ChatMessageDao
) : ViewModel() {

    // In a real app, this would come from a SessionManager
    private val currentUserId = "current_user_id" 

    val inboxState: StateFlow<List<InboxItem>> = getInboxUseCase(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    init {
        syncContacts()
    }

    fun syncContacts() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncContactsUseCase()
            _isSyncing.value = false
        }
    }

    fun deleteConversation(userId: String) {
        viewModelScope.launch {
            chatMessageDao.deleteConversation(currentUserId, userId)
        }
    }
}
