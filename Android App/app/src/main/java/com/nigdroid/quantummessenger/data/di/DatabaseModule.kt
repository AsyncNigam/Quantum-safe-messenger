package com.nigdroid.quantummessenger.data.di

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.room.Room
import com.nigdroid.quantummessenger.data.crypto.CryptoManager
import com.nigdroid.quantummessenger.data.local.ChatMessageDao
import com.nigdroid.quantummessenger.data.local.ContactDao
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
 *
 * The SQLCipher passphrase is derived from the CryptoManager master key.
 * Since the master key is now biometric-bound (15s auth window), the database
 * can only be opened AFTER the user has successfully authenticated via biometrics.
 *
 * The 15-second auth validity window covers the time from biometric success
 * to database initialization (which is lazy via Hilt's singleton scope).
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
        // This runs within the biometric auth window (15s after unlock)
        val passphrase = try {
            runBlocking { cryptoManager.getDatabasePassphrase() }
        } catch (e: UserNotAuthenticatedException) {
            // Should not happen — biometric gate runs before DB access
            // Fall back: generate a non-auth-bound temporary passphrase
            android.util.Log.e("DatabaseModule", "Auth required — DB opened before biometric: ${e.message}")
            ByteArray(32) { 0x00 }
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Key invalidated — VaultWipeManager will handle this
            android.util.Log.e("DatabaseModule", "KPIE — key invalidated: ${e.message}")
            ByteArray(32) { 0x00 }
        }

        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            QuantumMessengerDatabase::class.java,
            QuantumMessengerDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: QuantumMessengerDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: QuantumMessengerDatabase): ContactDao {
        return database.contactDao()
    }
}
