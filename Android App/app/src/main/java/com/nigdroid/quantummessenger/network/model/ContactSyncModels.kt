package com.nigdroid.quantummessenger.network.model

data class SyncRequest(
    val lastSyncTimestamp: Long,
    val page: Int = 0,
    val pageSize: Int = 100
)

data class SyncResponse(
    val contacts: List<RemoteContact>,
    val hasMore: Boolean,
    val nextCursor: String?,
    val serverTimestamp: Long
)

data class RemoteContact(
    val userId: String,
    val phoneNumber: String,
    val displayName: String?,
    val mlKemPublicKey: String,
    val mlDsaPublicKey: String,
    val x25519PublicKey: String,
    val ed25519PublicKey: String
)
