package com.nigdroid.quantummessenger.data.security

import android.content.Context
import com.nigdroid.quantummessenger.data.local.QuantumMessengerDatabase
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultWipeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val database: QuantumMessengerDatabase
) {

    companion object {
        private const val TAG = "VaultWipeManager"

        private val TINK_PREF_FILES    = listOf(
            "quantum_messenger_prefs_v2",
            "quantum_messenger_prefs_v3",
            "quantum_messenger_keys_v1"
        )
        private val MASTER_KEY_ALIASES = listOf("quantum_messenger_master_key_v2", "quantum_messenger_master_key_v3")
        private const val KEYSTORE_PREFIX   = "quantum_messenger_"
    }

    suspend fun executeZeroTrustWipe() = withContext(Dispatchers.IO) {
        android.util.Log.w(TAG, "EXECUTING ZERO-TRUST VAULT WIPE")

        closeDatabaseConnection()
        deleteDatabaseFiles()
        clearSessionData()
        deleteKeystoreKeys()
        clearTinkKeysets()

        android.util.Log.w(TAG, "Zero-trust wipe complete — app will require re-registration")
    }

    private fun closeDatabaseConnection() {
        try {
            if (database.isOpen) {
                database.close()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to close database: ${e.message}")
        }
    }

    private fun deleteDatabaseFiles() {
        try {
            val dbName = QuantumMessengerDatabase.DATABASE_NAME
            val dbFile    = context.getDatabasePath(dbName)
            val shmFile   = File(dbFile.path + "-shm")
            val walFile   = File(dbFile.path + "-wal")
            val journalFile = File(dbFile.path + "-journal")

            listOf(dbFile, shmFile, walFile, journalFile).forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to delete database: ${e.message}")
        }
    }

    private suspend fun clearSessionData() {
        try {
            sessionManager.setTextFingerprint(null)
            sessionManager.setDisplayName(null)
            sessionManager.setFcmToken(null)
            sessionManager.clearPublicKeys()
            sessionManager.setUserRegistered(false)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to clear session: ${e.message}")
        }
    }

    private fun deleteKeystoreKeys() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val aliasesToDelete = keyStore.aliases().toList().filter { alias ->
                alias.startsWith(KEYSTORE_PREFIX) || alias in MASTER_KEY_ALIASES
            }

            aliasesToDelete.forEach { alias ->
                try {
                    keyStore.deleteEntry(alias)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to delete alias $alias: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to clear Keystore: ${e.message}")
        }
    }

    private fun clearTinkKeysets() {
        try {
            TINK_PREF_FILES.forEach { prefFile ->
                val prefs = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to clear Tink prefs: ${e.message}")
        }
    }
}
