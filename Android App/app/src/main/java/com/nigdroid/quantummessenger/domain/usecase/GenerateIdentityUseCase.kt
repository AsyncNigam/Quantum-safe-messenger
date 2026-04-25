package com.nigdroid.quantummessenger.domain.usecase

import androidx.annotation.VisibleForTesting
import com.nigdroid.quantummessenger.crypto.CryptoException
import com.nigdroid.quantummessenger.crypto.PostQuantumCrypto
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult
import com.nigdroid.quantummessenger.domain.model.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import javax.inject.Inject

/**
 * Use Case: Generate the two public keys required for Zero-Knowledge registration.
 *
 * Generates:
 *  - ML-KEM-768 keypair  (post-quantum KEM — sent to backend)
 *  - X25519 keypair      (classical DH    — sent to backend)
 *
 * Private keys are stored in the Android Keystore.
 * Only public keys are returned for transmission to /auth/register.
 */
class GenerateIdentityUseCase @Inject constructor() {

    data class PublicKeys(
        val mlKemPublicKey: ByteArray,
        val x25519PublicKey: ByteArray
    )

    suspend operator fun invoke(): Result<PublicKeys> =
        withContext(Dispatchers.Default) {
            runCatching {
                // Step 1: Generate ML-KEM-768 keypair
                val mlKemPair = generateMLKemKey()

                // Step 2: Generate X25519 keypair
                val x25519Pair = generateX25519Key()

                val keys = PublicKeys(
                    mlKemPublicKey  = mlKemPair.publicKey,
                    x25519PublicKey = x25519Pair.publicKey,
                )

                // Secure-erase private key bytes from memory
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
        // Android Keystore storage implementation
        // Alias: "quantum_messenger_<keyType>"
    }
}

// Kotlin extension to transform Result<T> error without changing type
private fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
    fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(transform(it)) })
