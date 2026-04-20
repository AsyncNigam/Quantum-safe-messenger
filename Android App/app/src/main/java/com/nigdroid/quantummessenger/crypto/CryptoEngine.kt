package com.nigdroid.quantummessenger.crypto

data class KemKeypair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
)

data class EncapsulationResult(
    val ciphertext: ByteArray,
    val sharedSecret: ByteArray
)

object CryptoEngine {

    init {
        System.loadLibrary("native-lib")
    }

    // ── JNI declarations ──────────────────────────────────────────────────────
    private external fun generateKemKeypair(): Array<ByteArray>
    private external fun kemEncapsulate(recipientPublicKey: ByteArray): Array<ByteArray>
    private external fun kemDecapsulate(ciphertext: ByteArray, privateKey: ByteArray): ByteArray

    // ── Public API ────────────────────────────────────────────────────────────

    fun generateKeypair(): KemKeypair {
        val result = generateKemKeypair()
        return KemKeypair(
            publicKey  = result[0],
            privateKey = result[1]
        )
    }

    fun encapsulate(recipientPublicKey: ByteArray): EncapsulationResult {
        val result = kemEncapsulate(recipientPublicKey)
        return EncapsulationResult(
            ciphertext   = result[0],
            sharedSecret = result[1]
        )
    }

    fun decapsulate(ciphertext: ByteArray, privateKey: ByteArray): ByteArray {
        return kemDecapsulate(ciphertext, privateKey)
    }
}