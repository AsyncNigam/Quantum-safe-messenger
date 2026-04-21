package com.nigdroid.quantummessenger.crypto

/**
 * JNI Bridge for ML-KEM (CRYSTALS-Kyber) and ML-DSA (CRYSTALS-Dilithium)
 * Post-Quantum Cryptography
 *
 * This Kotlin interface bridges to C++ implementation for high-performance
 * post-quantum key generation and signature operations.
 *
 * NIST FIPS 203 (ML-KEM) and FIPS 204 (ML-DSA) compliant implementations.
 */
object PostQuantumCrypto {

    init {
        // Load the native C++ library
        System.loadLibrary("quantum_crypto")
    }

    /**
     * Generate ML-KEM keypair (NIST FIPS 203)
     *
     * ML-KEM is a Key Encapsulation Mechanism for key establishment.
     * Security level: 256-bit (ML-KEM-768 variant)
     *
     * @return Pair of (publicKey, privateKey) as ByteArrays
     * @throws CryptoException if generation fails
     */
    external fun generateMLKemKeypair(): Pair<ByteArray, ByteArray>

    /**
     * Generate ML-DSA keypair (NIST FIPS 204)
     *
     * ML-DSA is used for digital signature operations.
     * Security level: 256-bit (ML-DSA-87 variant)
     *
     * @return Pair of (publicKey, privateKey) as ByteArrays
     * @throws CryptoException if generation fails
     */
    external fun generateMLDsaKeypair(): Pair<ByteArray, ByteArray>

    /**
     * Sign data using ML-DSA private key
     *
     * @param message Data to sign
     * @param privateKey ML-DSA private key bytes
     * @return Digital signature (deterministic)
     */
    external fun signWithMLDsa(message: ByteArray, privateKey: ByteArray): ByteArray

    /**
     * Verify signature using ML-DSA public key
     *
     * @param message Original message
     * @param signature Signature to verify
     * @param publicKey ML-DSA public key bytes
     * @return true if signature is valid, false otherwise
     */
    external fun verifyMLDsaSignature(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean

    /**
     * Encapsulate (generate shared secret) using ML-KEM public key
     *
     * This creates a shared secret that can be decapsulated by the holder
     * of the corresponding private key.
     *
     * @param publicKey ML-KEM public key bytes
     * @return Pair of (encapsulation, sharedSecret)
     */
    external fun encapsulateMLKem(publicKey: ByteArray): Pair<ByteArray, ByteArray>

    /**
     * Decapsulate (recover shared secret) using ML-KEM private key
     *
     * @param encapsulation Encapsulation bytes from encapsulateMLKem()
     * @param privateKey ML-KEM private key bytes
     * @return Shared secret (should match the one from encapsulate)
     */
    external fun decapsulateMLKem(encapsulation: ByteArray, privateKey: ByteArray): ByteArray
}

/**
 * Exception thrown during cryptographic operations
 */
class CryptoException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

