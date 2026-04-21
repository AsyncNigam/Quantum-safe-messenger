package com.nigdroid.quantummessenger.domain.model

data class InboxItem(
    val userId: String,
    val displayName: String?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val avatarUrl: String? = null
)
