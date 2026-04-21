package com.nigdroid.quantummessenger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,
    val phoneNumber: String,
    val displayName: String?,
    val mlKemPublicKey: String,
    val mlDsaPublicKey: String,
    val x25519PublicKey: String,
    val ed25519PublicKey: String,
    val lastSeen: Long = System.currentTimeMillis()
)
