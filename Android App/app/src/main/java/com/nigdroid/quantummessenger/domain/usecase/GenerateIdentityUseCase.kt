package com.nigdroid.quantummessenger.domain.usecase

import android.content.Context
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
    internal fun generateMLKemKey(): KeyPair {
        return try {
            val (publicKey, privateKey) = PostQuantumCrypto.generateMLKemKeypair()
            storeKeyInKeystore("ml_kem", privateKey)
            KeyPair(publicKey = publicKey, privateKey = privateKey)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate ML-KEM keypair", e)
        }
    }

    @VisibleForTesting
    internal fun generateX25519Key(): KeyPair {
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

    @VisibleForTesting
    internal fun storeKeyInKeystore(keyType: String, keyMaterial: ByteArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val aad = "private_key_$keyType".toByteArray(Charsets.UTF_8)

        val encrypted = runBlocking { cryptoManager.encrypt(keyMaterial, aad) }
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)

        prefs.edit().putString("encrypted_pk_$keyType", encoded).apply()
    }

    suspend fun retrievePrivateKey(keyType: String): ByteArray? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = prefs.getString("encrypted_pk_$keyType", null) ?: return null
        val aad = "private_key_$keyType".toByteArray(Charsets.UTF_8)

        return try {
            val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
            cryptoManager.decrypt(ciphertext, aad)
        } catch (e: Exception) {
            android.util.Log.e("GenerateIdentity", "Failed to decrypt $keyType key: ${e.message}")
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "quantum_messenger_keys_v1"
    }
}

private fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
    fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(transform(it)) })
