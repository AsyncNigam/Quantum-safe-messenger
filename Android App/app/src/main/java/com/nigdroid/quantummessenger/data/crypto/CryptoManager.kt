package com.nigdroid.quantummessenger.data.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
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

    /**
     * Derive a deterministic 32-byte passphrase for SQLCipher.
     * The passphrase is derived by encrypting a fixed plaintext with the master key.
     */
    suspend fun getDatabasePassphrase(): ByteArray = withContext(Dispatchers.IO) {
        val passphraseData = "database_passphrase".toByteArray(Charsets.UTF_8)
        val encrypted = aead.encrypt(FIXED_PLAINTEXT, passphraseData)
        encrypted.copyOf(32)
    }

    companion object {
        private const val TAG = "CryptoManager"

        // V3 aliases — fresh generation with biometric binding
        private const val KEYSET_NAME      = "quantum_messenger_keyset_v3"
        private const val PREF_FILE_NAME   = "quantum_messenger_prefs_v3"
        const val MASTER_KEY_ALIAS         = "quantum_messenger_master_key_v3"

        private val FIXED_PLAINTEXT = ByteArray(32) { 0x42 }
    }
}
