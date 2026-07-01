package com.nigdroid.quantummessenger.domain.model

data class InboxItem(
    val userId: String,
    val displayName: String?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isBlocked: Boolean = false,
    val avatarUrl: String? = null
)
