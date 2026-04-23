package com.nigdroid.quantummessenger.domain.usecase

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import com.nigdroid.quantummessenger.crypto.CryptoException
import com.nigdroid.quantummessenger.crypto.PostQuantumCrypto
import com.nigdroid.quantummessenger.domain.model.Identity
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult
import com.nigdroid.quantummessenger.domain.model.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.UUID
import javax.inject.Inject

/**
 * Use Case: Generate secure identity for new user
 */
class GenerateIdentityUseCase @Inject constructor() {

    suspend operator fun invoke(identifier: String): IdentityGenerationResult =
        withContext(Dispatchers.Default) {
            try {
                // Validate input
                if (identifier.isBlank()) {
                    return@withContext IdentityGenerationResult.Error(
                        exception = IllegalArgumentException("Identifier cannot be empty"),
                        message = "Invalid identifier"
                    )
                }

                // Generate unique user ID
                val userId = UUID.randomUUID().toString()

                // Step 1: Generate ML-KEM keypair
                val mlKemPair = generateMLKemKey(userId)

                // Step 2: Generate ML-DSA keypair
                val mlDsaPair = generateMLDsaKey(userId)

                // Step 3: Generate X25519 keypair
                val x25519Pair = generateX25519Key(userId)

                // Step 4: Generate Ed25519 keypair
                val ed25519Pair = generateEd25519Key(userId)

                // Create identity object
                val identity = Identity(
                    userId = userId,
                    identifier = identifier,
                    mlKemPublicKey = mlKemPair.publicKey,
                    mlDsaPublicKey = mlDsaPair.publicKey,
                    x25519PublicKey = x25519Pair.publicKey,
                    ed25519PublicKey = ed25519Pair.publicKey,
                    keyStoreAliasPrefix = "quantum_${userId}_"
                )

                IdentityGenerationResult.Success(identity)

            } catch (e: CryptoException) {
                IdentityGenerationResult.Error(
                    exception = e,
                    message = "Cryptographic error: ${e.message}"
                )
            } catch (e: Exception) {
                IdentityGenerationResult.Error(
                    exception = e,
                    message = "Failed to generate identity: ${e.message}"
                )
            }
        }

    @VisibleForTesting
    internal fun generateMLKemKey(userId: String): KeyPair {
        return try {
            val keyPairResult = PostQuantumCrypto.generateMLKemKeypair()
            val publicKey = keyPairResult.first
            val privateKey = keyPairResult.second

            storeKeyInKeystore(userId, "ml_kem", privateKey)
            privateKey.fill(0)
            KeyPair(publicKey = publicKey, privateKey = null)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate ML-KEM keypair", e)
        }
    }

    @VisibleForTesting
    internal fun generateMLDsaKey(userId: String): KeyPair {
        return try {
            val keyPairResult = PostQuantumCrypto.generateMLDsaKeypair()
            val publicKey = keyPairResult.first
            val privateKey = keyPairResult.second

            storeKeyInKeystore(userId, "ml_dsa", privateKey)
            privateKey.fill(0)
            KeyPair(publicKey = publicKey, privateKey = null)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate ML-DSA keypair", e)
        }
    }

    @VisibleForTesting
    internal fun generateX25519Key(userId: String): KeyPair {
        return try {
            val keyGen = KeyPairGenerator.getInstance("X25519")
            val keyPair = keyGen.generateKeyPair()

            val publicKeyBytes = keyPair.public.encoded
            val privateKeyBytes = keyPair.private.encoded

            storeKeyInKeystore(userId, "x25519", privateKeyBytes)
            KeyPair(publicKey = publicKeyBytes, privateKey = null)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate X25519 keypair", e)
        }
    }

    @VisibleForTesting
    internal fun generateEd25519Key(userId: String): KeyPair {
        return try {
            val keyGen = KeyPairGenerator.getInstance("Ed25519")
            val keyPair = keyGen.generateKeyPair()

            val publicKeyBytes = keyPair.public.encoded
            val privateKeyBytes = keyPair.private.encoded

            storeKeyInKeystore(userId, "ed25519", privateKeyBytes)
            KeyPair(publicKey = publicKeyBytes, privateKey = null)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate Ed25519 keypair", e)
        }
    }

    @VisibleForTesting
    internal fun storeKeyInKeystore(userId: String, keyType: String, keyMaterial: ByteArray) {
        // Implementation for storage logic
    }
}
