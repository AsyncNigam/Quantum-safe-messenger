package com.nigdroid.quantummessenger.presentation.viewmodel

import com.nigdroid.quantummessenger.data.local.ContactEntity
import com.nigdroid.quantummessenger.domain.model.InboxItem

/**
 * UI State for the Home screen.
 */
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val inboxItems: List<InboxItem>,
        val contacts: List<ContactEntity> = emptyList(),
        val searchQuery: String = ""
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
