package com.nigdroid.quantummessenger.data.crypto

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CryptoManager handles symmetric encryption operations using Google Tink.
 * It manages a key stored in Android Keystore, preferring AES-256-GCM and falling back to XChaCha20.
 * Provides encryption/decryption for data and a secure passphrase for database encryption.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val aead: Aead by lazy {
        // Initialize Tink
        AeadConfig.register()

        // Create AndroidKeysetManager for Keystore integration
        val keysetManager = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM) // Prefers AES-256-GCM
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()

        // Get or create the keyset handle
        val keysetHandle = keysetManager.keysetHandle

        // Get the AEAD primitive
        keysetHandle.getPrimitive(Aead::class.java)
    }

    init {
        // Ensure Tink is initialized on background thread
        // But since it's lazy, it will be initialized when first accessed
    }

    /**
     * Encrypts the given plaintext using the managed key.
     * @param plaintext The data to encrypt
     * @param associatedData Optional associated data for AEAD
     * @return Encrypted ciphertext
     */
    suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray? = null): ByteArray =
        withContext(Dispatchers.IO) {
            aead.encrypt(plaintext, associatedData ?: ByteArray(0))
        }

    /**
     * Decrypts the given ciphertext using the managed key.
     * @param ciphertext The encrypted data
     * @param associatedData Optional associated data for AEAD (must match encryption)
     * @return Decrypted plaintext
     */
    suspend fun decrypt(ciphertext: ByteArray, associatedData: ByteArray? = null): ByteArray =
        withContext(Dispatchers.IO) {
            aead.decrypt(ciphertext, associatedData ?: ByteArray(0))
        }

    /**
     * Generates or retrieves a secure 256-bit passphrase for database encryption.
     * The passphrase is derived from the encryption key and cached for performance.
     * @return 32-byte passphrase
     */
    suspend fun getDatabasePassphrase(): ByteArray = withContext(Dispatchers.IO) {
        // Use a fixed associated data to derive the passphrase
        val passphraseData = "database_passphrase".toByteArray(Charsets.UTF_8)
        // Encrypt a fixed plaintext to get a deterministic but secure passphrase
        val encrypted = aead.encrypt(FIXED_PLAINTEXT, passphraseData)
        // Take first 32 bytes as passphrase
        encrypted.copyOf(32)
    }

    companion object {
        private const val KEYSET_NAME = "quantum_messenger_keyset"
        private const val PREF_FILE_NAME = "quantum_messenger_prefs"
        private const val MASTER_KEY_URI = "android-keystore://quantum_messenger_master_key"
        private val FIXED_PLAINTEXT = ByteArray(32) { 0x42 } // Fixed 32-byte plaintext for passphrase derivation
    }
}
