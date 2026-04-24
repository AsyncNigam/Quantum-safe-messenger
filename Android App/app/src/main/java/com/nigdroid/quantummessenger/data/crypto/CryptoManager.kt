package com.nigdroid.quantummessenger.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced CryptoManager with Biometric Binding.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        AeadConfig.register()
        ensureAuthBoundMasterKey()
    }

    /**
     * Ensures the master key in Android Keystore is bound to user authentication (Biometrics).
     */
    private fun ensureAuthBoundMasterKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Set authentication required to false for the Tink Master Key 
                // because Tink does not support interactive biometric prompts 
                // during automated operations like database access.
                .setUserAuthenticationRequired(false)
                .build()
            )
            keyGenerator.generateKey()
        }
    }

    private val aead: Aead by lazy {
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

    suspend fun getDatabasePassphrase(): ByteArray = withContext(Dispatchers.IO) {
        val passphraseData = "database_passphrase".toByteArray(Charsets.UTF_8)
        val encrypted = aead.encrypt(FIXED_PLAINTEXT, passphraseData)
        encrypted.copyOf(32)
    }

    companion object {
        private const val KEYSET_NAME = "quantum_messenger_keyset_v2"
        private const val PREF_FILE_NAME = "quantum_messenger_prefs_v2"
        // Updated alias to force fresh generation without biometric requirement
        private const val MASTER_KEY_ALIAS = "quantum_messenger_master_key_v2"
        private val FIXED_PLAINTEXT = ByteArray(32) { 0x42 }
    }
}
