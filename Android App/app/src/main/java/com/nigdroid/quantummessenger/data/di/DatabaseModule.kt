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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private class ReusableSupportFactory(
        private val passphrase: ByteArray
    ) : SupportSQLiteOpenHelper.Factory {
        override fun create(
            configuration: SupportSQLiteOpenHelper.Configuration
        ): SupportSQLiteOpenHelper {
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
