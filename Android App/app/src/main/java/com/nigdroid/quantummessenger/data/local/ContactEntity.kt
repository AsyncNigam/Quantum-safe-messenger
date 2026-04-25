package com.nigdroid.quantummessenger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,       // = textFingerprint of the contact
    val displayName: String? = null,
    val mlKemPublicKey: String,
    val x25519PublicKey: String,
    val addedAt: Long = System.currentTimeMillis()
)
