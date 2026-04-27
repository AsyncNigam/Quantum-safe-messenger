package com.nigdroid.quantummessenger.data.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
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
 * The SQLCipher passphrase is derived from the CryptoManager using a
 * non-auth-bound Keystore key — this means the passphrase is ALWAYS
 * accessible (no biometric timing window required).
 *
 * The biometric gate at the app level (LockedScreen) protects runtime
 * access; the DB passphrase key only protects data-at-rest on disk.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * A reusable factory wrapper that creates a fresh [SupportFactory] (with a fresh
     * clone of the passphrase) every time Room needs to open or reopen the database.
     *
     * **Why this is needed:**
     * SQLCipher's [SupportFactory] zeros out the passphrase byte array after the
     * first database open — even when `clearPassphrase = false` (a known issue in
     * SQLCipher 4.5.x). When Room later closes and reopens the database (e.g. after
     * process death / app restart), it reuses the same [SupportFactory] instance
     * whose internal passphrase is now all zeros, causing the
     * "passphrase appears to be cleared" IllegalStateException.
     *
     * By wrapping in our own Factory, we guarantee a fresh passphrase copy on every
     * `create()` call, making the database resilient to Room's open/close lifecycle.
     */
    private class ReusableSupportFactory(
        private val passphrase: ByteArray
    ) : SupportSQLiteOpenHelper.Factory {
        override fun create(
            configuration: SupportSQLiteOpenHelper.Configuration
        ): SupportSQLiteOpenHelper {
            // Provide a fresh SupportFactory + fresh passphrase clone each time
            // Room needs to open the database. The clone is consumed (and possibly
            // zeroed) by SupportFactory, but our cached `passphrase` stays intact.
            return SupportFactory(passphrase.clone(), null, false)
                .create(configuration)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): QuantumMessengerDatabase {
        // Get the passphrase for SQLCipher.
        // This now uses a non-auth-bound Keystore key, so it NEVER throws
        // UserNotAuthenticatedException or requires a biometric timing window.
        val passphrase = runBlocking { cryptoManager.getDatabasePassphrase() }

        return Room.databaseBuilder(
            context,
            QuantumMessengerDatabase::class.java,
            QuantumMessengerDatabase.DATABASE_NAME
        )
            .openHelperFactory(ReusableSupportFactory(passphrase))
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
