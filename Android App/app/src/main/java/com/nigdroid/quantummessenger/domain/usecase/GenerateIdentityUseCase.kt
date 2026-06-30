package com.nigdroid.quantummessenger.domain.usecase

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.nigdroid.quantummessenger.crypto.CryptoException
import com.nigdroid.quantummessenger.crypto.PostQuantumCrypto
import com.nigdroid.quantummessenger.data.crypto.CryptoManager
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult
import com.nigdroid.quantummessenger.domain.model.KeyPair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

class GenerateIdentityUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {

    data class PublicKeys(
        val mlKemPublicKey: ByteArray,
        val x25519PublicKey: ByteArray
    )

    suspend operator fun invoke(): Result<PublicKeys> =
        withContext(Dispatchers.Default) {
            runCatching {
                val mlKemPair = generateMLKemKey()
                val x25519Pair = generateX25519Key()

                val keys = PublicKeys(
                    mlKemPublicKey  = mlKemPair.publicKey,
                    x25519PublicKey = x25519Pair.publicKey,
                )

                mlKemPair.clear()
                x25519Pair.clear()

                keys
            }.mapError { e ->
                when (e) {
                    is CryptoException -> e
                    else -> CryptoException("Key generation failed: ${e.message}", e)
                }
            }
        }

    @VisibleForTesting
    internal suspend fun generateMLKemKey(): KeyPair {
        return try {
            val (publicKey, privateKey) = PostQuantumCrypto.generateMLKemKeypair()
            storeKeyInKeystore("ml_kem", privateKey)
            KeyPair(publicKey = publicKey, privateKey = privateKey)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate ML-KEM keypair", e)
        }
    }

    @VisibleForTesting
    internal suspend fun generateX25519Key(): KeyPair {
        return try {
            val keyGen  = KeyPairGenerator.getInstance("X25519")
            val keyPair = keyGen.generateKeyPair()
            val pub     = keyPair.public.encoded
            val priv    = keyPair.private.encoded
            storeKeyInKeystore("x25519", priv)
            KeyPair(publicKey = pub, privateKey = priv)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate X25519 keypair", e)
        }
    }

    /**
     * Stores private key material using a NON-auth-bound AES key.
     *
     * Previously this used `cryptoManager.encrypt()` which relies on an
     * auth-bound Tink AEAD master key.  On a fresh install the user has
     * NOT yet biometrically authenticated, so the auth-bound key throws
     * `UserNotAuthenticatedException`.
     *
     * Fix: use a dedicated, non-auth-bound AES-GCM key stored in
     * Android Keystore, just like `CryptoManager.ensureDbPassphraseKey()`.
     */
    @VisibleForTesting
    internal fun storeKeyInKeystore(keyType: String, keyMaterial: ByteArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val encrypted = encryptWithKeyStoreKey(keyMaterial)
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)

        prefs.edit().putString("encrypted_pk_v2_$keyType", encoded).apply()
    }

    fun retrievePrivateKey(keyType: String): ByteArray? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Try new v2 format first (non-auth-bound)
        val v2Encoded = prefs.getString("encrypted_pk_v2_$keyType", null)
        if (v2Encoded != null) {
            return try {
                val cipherBytes = Base64.decode(v2Encoded, Base64.NO_WRAP)
                decryptWithKeyStoreKey(cipherBytes)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to decrypt v2 $keyType key: ${e.message}")
                null
            }
        }

        // Fall back to old v1 format (auth-bound Tink) for existing installs
        val v1Encoded = prefs.getString("encrypted_pk_$keyType", null) ?: return null
        return try {
            val ciphertext = Base64.decode(v1Encoded, Base64.NO_WRAP)
            val aad = "private_key_$keyType".toByteArray(Charsets.UTF_8)
            runBlocking { cryptoManager.decrypt(ciphertext, aad) }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to decrypt v1 $keyType key: ${e.message}")
            null
        }
    }

    // ── Non-auth-bound AES-GCM encryption ─────────────────────────────────────

    private fun ensureKeyStoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(PK_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                PK_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)   // ← no biometric needed
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun encryptWithKeyStoreKey(plaintext: ByteArray): ByteArray {
        ensureKeyStoreKey()
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(PK_KEY_ALIAS, null) as SecretKey

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext)
        return iv + encrypted   // 12-byte IV + ciphertext+tag
    }

    private fun decryptWithKeyStoreKey(data: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(PK_KEY_ALIAS, null) as SecretKey

        val iv = data.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = data.sliceArray(GCM_IV_LENGTH until data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    companion object {
        private const val TAG = "GenerateIdentity"
        private const val PREFS_NAME = "quantum_messenger_keys_v1"
        private const val PK_KEY_ALIAS = "quantum_messenger_pk_storage_v1"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BIT_LENGTH = 128
    }
}

private fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
    fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(transform(it)) })
