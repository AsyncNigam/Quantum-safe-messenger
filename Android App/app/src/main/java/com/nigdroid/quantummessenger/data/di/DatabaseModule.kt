package com.nigdroid.quantummessenger.data.di

import android.content.Context
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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "DatabaseModule"

    init {
        System.loadLibrary("sqlcipher")
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): QuantumMessengerDatabase {
        val passphrase = runBlocking { cryptoManager.getDatabasePassphrase() }

        val db = buildDatabase(context, passphrase)

        // Eagerly verify that the database can actually be opened with this
        // passphrase.  After an account deletion + restart, the old .db file
        // may still be on disk but encrypted with a different key.
        return try {
            // Force Room to open the underlying SQLCipher database now.
            db.openHelper.writableDatabase
            db
        } catch (e: Exception) {
            // Covers SQLiteNotADatabaseException (wrong passphrase / corrupt db)
            // and any other open-time error.
            android.util.Log.w(TAG, "Database open failed — deleting corrupt file and recreating: ${e.message}")
            try { db.close() } catch (_: Exception) {}

            // Delete the database files
            val dbFile = context.getDatabasePath(QuantumMessengerDatabase.DATABASE_NAME)
            listOf(dbFile, File(dbFile.path + "-shm"), File(dbFile.path + "-wal"), File(dbFile.path + "-journal"))
                .forEach { it.delete() }

            // Generate a fresh passphrase (the old one decrypted a now-deleted DB)
            val freshPassphrase = runBlocking { cryptoManager.resetDatabasePassphrase() }
            buildDatabase(context, freshPassphrase)
        }
    }

    private fun buildDatabase(context: Context, passphrase: ByteArray): QuantumMessengerDatabase {
        return Room.databaseBuilder(
            context,
            QuantumMessengerDatabase::class.java,
            QuantumMessengerDatabase.DATABASE_NAME
        )
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
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
