package com.nigdroid.quantummessenger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Encrypted Room database using SQLCipher for storing chat messages.
 */
@Database(
    entities = [ChatMessageEntity::class, ContactEntity::class],
    version = 3,
    exportSchema = false
)
abstract class QuantumMessengerDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun contactDao(): ContactDao

    companion object {
        const val DATABASE_NAME = "quantum_messenger.db"
    }
}
