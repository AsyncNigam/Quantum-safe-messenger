package com.nigdroid.quantummessenger.crypto

/**
 * Results for cryptographic operations
 */
data class KemEncapsulation(val encapsulation: ByteArray, val sharedSecret: ByteArray)

/**
 * JNI Bridge for ML-KEM (CRYSTALS-Kyber) and ML-DSA (CRYSTALS-Dilithium)
 * Post-Quantum Cryptography
 */
object PostQuantumCrypto {

    init {
        System.loadLibrary("quantum_crypto")
    }

    /**
     * Generate ML-KEM keypair (NIST FIPS 203)
     * @return Pair containing publicKey and privateKey
     */
    external fun generateMLKemKeypair(): Pair<ByteArray, ByteArray>

    /**
     * Generate ML-DSA keypair (NIST FIPS 204)
     * @return Pair containing publicKey and privateKey
     */
    external fun generateMLDsaKeypair(): Pair<ByteArray, ByteArray>

    /**
     * Sign data using ML-DSA private key
     */
    external fun signWithMLDsa(message: ByteArray, privateKey: ByteArray): ByteArray

    /**
     * Verify signature using ML-DSA public key
     */
    external fun verifyMLDsaSignature(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean

    /**
     * Encapsulate (generate shared secret) using ML-KEM public key
     * @return KemEncapsulation containing encapsulation and sharedSecret
     */
    external fun encapsulateMLKem(publicKey: ByteArray): KemEncapsulation

    /**
     * Decapsulate (recover shared secret) using ML-KEM private key
     */
    external fun decapsulateMLKem(encapsulation: ByteArray, privateKey: ByteArray): ByteArray
}

/**
 * Exception thrown during cryptographic operations
 */
class CryptoException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
