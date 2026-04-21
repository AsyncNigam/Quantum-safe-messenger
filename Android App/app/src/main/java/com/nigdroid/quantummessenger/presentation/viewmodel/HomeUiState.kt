package com.nigdroid.quantummessenger.presentation.viewmodel

import com.nigdroid.quantummessenger.domain.model.InboxItem

/**
 * UI State for the Home screen.
 */
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val inboxItems: List<InboxItem>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
