package com.nigdroid.quantummessenger.crypto

/**
 * Legacy wrapper for PostQuantumCrypto to maintain compatibility with existing code.
 * All new code should use PostQuantumCrypto directly.
 */
object CryptoEngine {

    fun generateKeypair(): KemKeypair {
        val (pub, priv) = PostQuantumCrypto.generateMLKemKeypair()
        return KemKeypair(pub, priv)
    }

    fun encapsulate(recipientPublicKey: ByteArray): EncapsulationResult {
        val result = PostQuantumCrypto.encapsulateMLKem(recipientPublicKey)
        return EncapsulationResult(result.encapsulation, result.sharedSecret)
    }

    fun decapsulate(ciphertext: ByteArray, privateKey: ByteArray): ByteArray {
        return PostQuantumCrypto.decapsulateMLKem(ciphertext, privateKey)
    }
}

data class KemKeypair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
)

data class EncapsulationResult(
    val ciphertext: ByteArray,
    val sharedSecret: ByteArray
)
