package com.nigdroid.quantummessenger.data.di

import android.content.Context
import androidx.room.Room
import com.nigdroid.quantummessenger.data.crypto.CryptoManager
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.QuantumMessengerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): QuantumMessengerDatabase {
        // Get the passphrase for SQLCipher
        val passphrase = runBlocking { cryptoManager.getDatabasePassphrase() }
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            QuantumMessengerDatabase::class.java,
            QuantumMessengerDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .build()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: QuantumMessengerDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }
}
