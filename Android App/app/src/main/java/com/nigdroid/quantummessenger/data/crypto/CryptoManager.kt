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

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        AeadConfig.register()
    }

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
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                specBuilder.setUserAuthenticationParameters(
                    15,
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

    fun validateKeyIntegrity(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                return true
            }

            val key = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            true
        } catch (e: KeyPermanentlyInvalidatedException) {
            android.util.Log.w(TAG, "KEY PERMANENTLY INVALIDATED — biometric enrollment changed")
            false
        } catch (e: android.security.keystore.UserNotAuthenticatedException) {
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Key validation error: ${e.message}")
            false
        }
    }

    private val aead: Aead by lazy {
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
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun encryptWithDbKey(plaintext: ByteArray): ByteArray {
        ensureDbPassphraseKey()
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(DB_KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext)
        return iv + encrypted
    }

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

    suspend fun getDatabasePassphrase(): ByteArray = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

        val newFormatCiphertext = prefs.getString(DB_PASSPHRASE_KEY_V2, null)
        if (newFormatCiphertext != null) {
            val cipherBytes = Base64.decode(newFormatCiphertext, Base64.NO_WRAP)
            return@withContext decryptWithDbKey(cipherBytes)
        }

        val oldFormatCiphertext = prefs.getString(DB_PASSPHRASE_KEY_V1, null)
        if (oldFormatCiphertext != null) {
            android.util.Log.i(TAG, "Migrating DB passphrase from auth-bound to non-auth-bound key")
            try {
                val oldCipherBytes = Base64.decode(oldFormatCiphertext, Base64.NO_WRAP)
                val passphrase = aead.decrypt(oldCipherBytes, DB_AAD)

                val newEncrypted = encryptWithDbKey(passphrase)
                val newEncoded = Base64.encodeToString(newEncrypted, Base64.NO_WRAP)
                prefs.edit()
                    .putString(DB_PASSPHRASE_KEY_V2, newEncoded)
                    .apply()

                return@withContext passphrase
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Migration failed: ${e.message}. Will retry on next launch.")
                val oldCipherBytes = Base64.decode(oldFormatCiphertext, Base64.NO_WRAP)
                return@withContext aead.decrypt(oldCipherBytes, DB_AAD)
            }
        }

        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encrypted = encryptWithDbKey(passphrase)
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(DB_PASSPHRASE_KEY_V2, encoded).apply()

        passphrase
    }

    companion object {
        private const val TAG = "CryptoManager"
        private const val KEYSET_NAME      = "quantum_messenger_keyset_v3"
        private const val PREF_FILE_NAME   = "quantum_messenger_prefs_v3"
        const val MASTER_KEY_ALIAS         = "quantum_messenger_master_key_v3"
        private const val DB_PASSPHRASE_KEY_V1 = "db_passphrase_ciphertext_v1"
        private const val DB_PASSPHRASE_KEY_V2 = "db_passphrase_ciphertext_v2"
        private val DB_AAD = "sqlcipher_passphrase".toByteArray(Charsets.UTF_8)
        private const val DB_KEY_ALIAS = "quantum_messenger_db_key_v1"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BIT_LENGTH = 128
    }
}
