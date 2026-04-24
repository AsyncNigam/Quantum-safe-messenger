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

    suspend operator fun invoke(identifier: String, userId: String): IdentityGenerationResult =
        withContext(Dispatchers.Default) {
            try {
                // Validate input
                if (identifier.isBlank() || userId.isBlank()) {
                    return@withContext IdentityGenerationResult.Error(
                        exception = IllegalArgumentException("Identifier and UserID cannot be empty"),
                        message = "Invalid identity data"
                    )
                }

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

                // Generate signatures of the public keys to prove possession and bind them
                val bundleToSign = identity.mlKemPublicKey + identity.mlDsaPublicKey + 
                                  identity.x25519PublicKey + identity.ed25519PublicKey
                
                val mlDsaSignature = PostQuantumCrypto.signWithMLDsa(bundleToSign, mlDsaPair.privateKey ?: ByteArray(0))
                val ed25519Signature = signWithEd25519(bundleToSign, ed25519Pair.privateKey ?: ByteArray(0))

                // Clear private keys from memory immediately after signing
                mlKemPair.clear()
                mlDsaPair.clear()
                x25519Pair.clear()
                ed25519Pair.clear()

                IdentityGenerationResult.Success(identity.copy(
                    mlDsaSignature = mlDsaSignature,
                    ed25519Signature = ed25519Signature
                ))

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
            KeyPair(publicKey = publicKey, privateKey = privateKey)
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
            KeyPair(publicKey = publicKey, privateKey = privateKey)
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
            KeyPair(publicKey = publicKeyBytes, privateKey = privateKeyBytes)
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
            KeyPair(publicKey = publicKeyBytes, privateKey = privateKeyBytes)
        } catch (e: Exception) {
            throw CryptoException("Failed to generate Ed25519 keypair", e)
        }
    }

    @VisibleForTesting
    internal fun storeKeyInKeystore(userId: String, keyType: String, keyMaterial: ByteArray) {
        // Implementation for storage logic
    }

    private fun signWithEd25519(data: ByteArray, privateKeyBytes: ByteArray): ByteArray {
        return try {
            if (privateKeyBytes.isEmpty()) return ByteArray(0)
            val kf = java.security.KeyFactory.getInstance("Ed25519")
            val privKey = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))
            val sig = java.security.Signature.getInstance("Ed25519")
            sig.initSign(privKey)
            sig.update(data)
            sig.sign()
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}
