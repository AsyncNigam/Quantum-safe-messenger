package com.nigdroid.quantummessenger.crypto

data class KemEncapsulation(val encapsulation: ByteArray, val sharedSecret: ByteArray)

object PostQuantumCrypto {

    init {
        System.loadLibrary("quantum_crypto")
    }

    private external fun jniGenerateKemKeypair(): Array<ByteArray>
    private external fun jniKemEncapsulate(recipientPublicKey: ByteArray): Array<ByteArray>
    private external fun jniKemDecapsulate(ciphertext: ByteArray, privateKey: ByteArray): ByteArray

    fun generateMLKemKeypair(): Pair<ByteArray, ByteArray> {
        return try {
            val result = jniGenerateKemKeypair()
            Pair(result[0], result[1])
        } catch (e: Exception) {
            throw CryptoException("Failed to generate ML-KEM keypair", e)
        }
    }

    fun encapsulateMLKem(publicKey: ByteArray): KemEncapsulation {
        return try {
            val result = jniKemEncapsulate(publicKey)
            KemEncapsulation(result[0], result[1])
        } catch (e: Exception) {
            throw CryptoException("Failed to encapsulate with ML-KEM", e)
        }
    }

    fun decapsulateMLKem(ciphertext: ByteArray, privateKey: ByteArray): ByteArray {
        return try {
            jniKemDecapsulate(ciphertext, privateKey)
        } catch (e: Exception) {
            throw CryptoException("Failed to decapsulate ML-KEM", e)
        }
    }

    // ML-DSA stubs — not yet included in liboqs binding

    fun generateMLDsaKeypair(): Pair<ByteArray, ByteArray> {
        val dummyPublic = ByteArray(32) { 1.toByte() }
        val dummyPrivate = ByteArray(32) { 2.toByte() }
        return Pair(dummyPublic, dummyPrivate)
    }

    fun signWithMLDsa(message: ByteArray, privateKey: ByteArray): ByteArray {
        return ByteArray(64) { 3.toByte() }
    }

    fun verifyMLDsaSignature(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean {
        return false
    }
}

class CryptoException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
