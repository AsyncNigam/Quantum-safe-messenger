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

/**
 * VaultWipeManager — Zero-Trust Security Response
 *
 * Executes a full cryptographic wipe when the biometric enrollment has changed,
 * which permanently invalidates our Keystore-bound master key.
 *
 * The wipe protocol:
 *   1. Close the open Room/SQLCipher database connection
 *   2. Delete the encrypted Room/SQLCipher database file
 *   3. Clear all DataStore preferences (session, fingerprint, display name, keys)
 *   4. Delete all Android Keystore aliases matching our prefix
 *   5. Clear Tink keyset SharedPreferences
 *
 * After this, the user must re-register via AuthScreen to generate a new identity.
 */
@Singleton
class VaultWipeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val database: QuantumMessengerDatabase
) {

    companion object {
        private const val TAG = "VaultWipeManager"

        // Must match CryptoManager and GenerateIdentityUseCase constants
        private val TINK_PREF_FILES    = listOf(
            "quantum_messenger_prefs_v2",
            "quantum_messenger_prefs_v3",
            "quantum_messenger_keys_v1"   // Encrypted PQ private keys
        )
        private val MASTER_KEY_ALIASES = listOf("quantum_messenger_master_key_v2", "quantum_messenger_master_key_v3")

        // Keystore alias prefix for PQ key material
        private const val KEYSTORE_PREFIX   = "quantum_messenger_"
    }

    /**
     * Execute a full zero-trust wipe.
     *
     * After this call, the app is in a factory-reset-like state.
     * The next launch will route to AuthScreen for identity re-generation.
     */
    suspend fun executeZeroTrustWipe() = withContext(Dispatchers.IO) {
        android.util.Log.w(TAG, "⚠️ EXECUTING ZERO-TRUST VAULT WIPE")

        // ── 1. Close the Room database connection ──────────────────────────
        closeDatabaseConnection()

        // ── 2. Delete the Room/SQLCipher database ──────────────────────────
        deleteDatabaseFiles()

        // ── 3. Clear DataStore (session, fingerprint, keys, display name) ──
        clearSessionData()

        // ── 4. Delete all Keystore keys ────────────────────────────────────
        deleteKeystoreKeys()

        // ── 5. Clear Tink keyset from SharedPreferences ────────────────────
        clearTinkKeysets()

        android.util.Log.w(TAG, "✅ Zero-trust wipe complete — app will require re-registration")
    }

    private fun closeDatabaseConnection() {
        try {
            if (database.isOpen) {
                database.close()
                android.util.Log.d(TAG, "Closed Room database connection")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to close database: ${e.message}")
        }
    }

    private fun deleteDatabaseFiles() {
        try {
            val dbName = QuantumMessengerDatabase.DATABASE_NAME
            // Room creates: <name>, <name>-shm, <name>-wal
            val dbFile    = context.getDatabasePath(dbName)
            val shmFile   = File(dbFile.path + "-shm")
            val walFile   = File(dbFile.path + "-wal")
            val journalFile = File(dbFile.path + "-journal")

            listOf(dbFile, shmFile, walFile, journalFile).forEach { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    android.util.Log.d(TAG, "Deleted ${file.name}: $deleted")
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
                    android.util.Log.d(TAG, "Deleted Keystore alias: $alias")
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
                android.util.Log.d(TAG, "Cleared Tink keyset: $prefFile")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to clear Tink prefs: ${e.message}")
        }
    }
}
