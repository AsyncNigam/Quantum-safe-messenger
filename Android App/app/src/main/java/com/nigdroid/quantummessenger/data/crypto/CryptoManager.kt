package com.nigdroid.quantummessenger.data.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-grade CryptoManager with Biometric-Bound Keystore Keys.
 *
 * Security architecture:
 *   • Master AES-256-GCM key lives in Android Keystore
 *   • Key requires user authentication (biometric/device credential)
 *   • Key is INVALIDATED if biometric enrollment changes (Zero-Trust / Signal model)
 *   • 15s auth validity window allows Tink operations after biometric unlock
 *   • KeyPermanentlyInvalidatedException triggers full vault wipe
 *
 * The CryptoManager does NOT automatically prompt for biometrics — the caller
 * (MainActivity) must ensure the user has been authenticated before calling
 * encrypt/decrypt. The 15s auth window covers the time between biometric
 * prompt success and the first crypto operation.
 *
 * DB Passphrase architecture (separate from message encryption):
 *   • Uses a NON-auth-bound Keystore key (always accessible after device unlock)
 *   • The biometric gate at the app level already protects runtime access
 *   • This key only protects data-at-rest (on-disk SQLCipher database)
 *   • Eliminates the 15s auth-window race condition that caused
 *     "passphrase appears to be cleared" errors on app restart
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        AeadConfig.register()
    }

    /**
     * Ensures the master key exists in Android Keystore.
     * Called AFTER biometric authentication succeeds.
     *
     * The key is:
     *   • AES-256-GCM
     *   • Bound to user authentication (biometric or device credential)
     *   • Invalidated on biometric enrollment change (Zero-Trust)
     *   • Valid for 15 seconds after authentication
     */
    fun ensureAuthBoundMasterKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )

            val specBuilder = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // ── Biometric Binding ──────────────────────────────────────
                .setUserAuthenticationRequired(true)
                // ── Zero-Trust: Invalidate if biometrics change ────────────
                .setInvalidatedByBiometricEnrollment(true)

            // API 30+ : duration-based auth validity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                specBuilder.setUserAuthenticationParameters(
                    15,  // 15 second window after biometric success
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                specBuilder.setUserAuthenticationValidityDurationSeconds(15)
            }

            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
        }
    }

    /**
     * Validates that the master key is still usable.
     *
     * If biometric enrollment has changed since the key was created,
     * the Android Keystore will throw KeyPermanentlyInvalidatedException
     * when we try to initialize a Cipher with the key.
     *
     * @return true if key is intact; false if biometrics changed (KPIE)
     * @throws Exception for unexpected Keystore errors
     */
    fun validateKeyIntegrity(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                // Key doesn't exist yet — not compromised, just first launch
                return true
            }

            val key = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey

            // Attempt to init a cipher — this is where KPIE surfaces
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            true  // Key is intact
        } catch (e: KeyPermanentlyInvalidatedException) {
            android.util.Log.w(TAG, "🔴 KEY PERMANENTLY INVALIDATED — biometric enrollment changed")
            false
        } catch (e: android.security.keystore.UserNotAuthenticatedException) {
            // User hasn't authenticated yet — key exists but needs biometric
            // This is expected BEFORE biometric prompt; key is still valid
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Key validation error: ${e.message}")
            // Conservative: assume compromised for unknown errors
            false
        }
    }

    // ── Tink AEAD (lazy init after biometric unlock) ──────────────────────────

    private val aead: Aead by lazy {
        // Ensure the master key exists before Tink tries to use it
        ensureAuthBoundMasterKey()

        val keysetManager = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
            .build()
        keysetManager.keysetHandle.getPrimitive(Aead::class.java)
    }

    suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray? = null): ByteArray =
        withContext(Dispatchers.IO) {
            aead.encrypt(plaintext, associatedData ?: ByteArray(0))
        }

    suspend fun decrypt(ciphertext: ByteArray, associatedData: ByteArray? = null): ByteArray =
        withContext(Dispatchers.IO) {
            aead.decrypt(ciphertext, associatedData ?: ByteArray(0))
        }

    // ═════════════════════════════════════════════════════════════════════════
    // DB Passphrase — uses a NON-auth-bound key (always accessible)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Ensures the DB passphrase Keystore key exists.
     * This key is NOT auth-bound — it's always accessible once the device
     * is unlocked (screen lock). The biometric gate protects app-level access;
     * this key only needs to protect data-at-rest on disk.
     */
    private fun ensureDbPassphraseKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(DB_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                DB_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // NOT auth-bound — always accessible after device unlock
                // NOT invalidated by biometric enrollment changes
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    /**
     * Encrypts data with the non-auth-bound DB key.
     * Returns IV (12 bytes) + ciphertext concatenated.
     */
    private fun encryptWithDbKey(plaintext: ByteArray): ByteArray {
        ensureDbPassphraseKey()
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(DB_KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv  // 12 bytes for GCM
        val encrypted = cipher.doFinal(plaintext)
        // Concatenate: [12-byte IV][ciphertext+tag]
        return iv + encrypted
    }

    /**
     * Decrypts data encrypted by [encryptWithDbKey].
     * Expects [12-byte IV][ciphertext+tag] format.
     */
    private fun decryptWithDbKey(data: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(DB_KEY_ALIAS, null) as SecretKey

        val iv = data.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = data.sliceArray(GCM_IV_LENGTH until data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * Returns a deterministic 32-byte passphrase for SQLCipher.
     *
     * Strategy:
     *   1. Try new format (non-auth-bound key) — always works, no auth window needed
     *   2. Migrate from old format (auth-bound Tink AEAD) — one-time migration
     *   3. First launch — generate new passphrase with non-auth-bound key
     *
     * The non-auth-bound key eliminates the "passphrase appears to be cleared"
     * error that occurred when the biometric auth window expired before the
     * database was opened on app restart.
     */
    suspend fun getDatabasePassphrase(): ByteArray = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

        // ── 1. Try new non-auth-bound format (v2) ────────────────────────
        val newFormatCiphertext = prefs.getString(DB_PASSPHRASE_KEY_V2, null)
        if (newFormatCiphertext != null) {
            val cipherBytes = Base64.decode(newFormatCiphertext, Base64.NO_WRAP)
            return@withContext decryptWithDbKey(cipherBytes)
        }

        // ── 2. Migrate from old auth-bound format (v1) ───────────────────
        val oldFormatCiphertext = prefs.getString(DB_PASSPHRASE_KEY_V1, null)
        if (oldFormatCiphertext != null) {
            android.util.Log.i(TAG, "Migrating DB passphrase from auth-bound to non-auth-bound key")
            try {
                // Decrypt with old Tink AEAD (needs auth window — available right after biometric)
                val oldCipherBytes = Base64.decode(oldFormatCiphertext, Base64.NO_WRAP)
                val passphrase = aead.decrypt(oldCipherBytes, DB_AAD)

                // Re-encrypt with non-auth-bound key and store as v2
                val newEncrypted = encryptWithDbKey(passphrase)
                val newEncoded = Base64.encodeToString(newEncrypted, Base64.NO_WRAP)
                prefs.edit()
                    .putString(DB_PASSPHRASE_KEY_V2, newEncoded)
                    .apply()

                android.util.Log.i(TAG, "✅ DB passphrase migration complete")
                return@withContext passphrase
            } catch (e: Exception) {
                // Migration failed (auth window expired, key invalidated, etc.)
                // The old passphrase is still there for next attempt
                android.util.Log.e(TAG, "Migration failed: ${e.message}. Will retry on next launch.")
                // Try to decrypt with old method anyway — might work if AEAD is cached
                val oldCipherBytes = Base64.decode(oldFormatCiphertext, Base64.NO_WRAP)
                return@withContext aead.decrypt(oldCipherBytes, DB_AAD)
            }
        }

        // ── 3. First launch — generate new passphrase ────────────────────
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encrypted = encryptWithDbKey(passphrase)
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(DB_PASSPHRASE_KEY_V2, encoded).apply()

        android.util.Log.i(TAG, "✅ Generated new DB passphrase (non-auth-bound)")
        passphrase
    }

    companion object {
        private const val TAG = "CryptoManager"

        // V3 aliases — fresh generation with biometric binding (for message encryption)
        private const val KEYSET_NAME      = "quantum_messenger_keyset_v3"
        private const val PREF_FILE_NAME   = "quantum_messenger_prefs_v3"
        const val MASTER_KEY_ALIAS         = "quantum_messenger_master_key_v3"

        // DB passphrase keys
        private const val DB_PASSPHRASE_KEY_V1 = "db_passphrase_ciphertext_v1"  // old auth-bound
        private const val DB_PASSPHRASE_KEY_V2 = "db_passphrase_ciphertext_v2"  // new non-auth-bound
        private val DB_AAD = "sqlcipher_passphrase".toByteArray(Charsets.UTF_8)

        // Non-auth-bound Keystore alias for DB passphrase only
        private const val DB_KEY_ALIAS = "quantum_messenger_db_key_v1"

        // GCM constants
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BIT_LENGTH = 128
    }
}
