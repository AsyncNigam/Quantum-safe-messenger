package com.nigdroid.quantummessenger.crypto

/**
 * Results for cryptographic operations
 */
data class KemEncapsulation(val encapsulation: ByteArray, val sharedSecret: ByteArray)

/**
 * JNI Bridge for ML-KEM (CRYSTALS-Kyber) and ML-DSA (CRYSTALS-Dilithium)
 * Post-Quantum Cryptography
 *
 * This class wraps the native quantum_crypto library (liboqs-based implementation).
 * ML-KEM is fully implemented; ML-DSA is a future enhancement.
 */
object PostQuantumCrypto {

    init {
        System.loadLibrary("quantum_crypto")
    }

    // ──── JNI Declarations for ML-KEM ────────────────────────────────────────
    // These map to the CryptoEngine functions in liboqs via JNI

    /**
     * Native JNI function: Generate ML-KEM keypair
     * Returns Array<ByteArray> where [0] = publicKey, [1] = privateKey
     */
    private external fun jniGenerateKemKeypair(): Array<ByteArray>

    /**
     * Native JNI function: ML-KEM Encapsulate
     * Returns Array<ByteArray> where [0] = ciphertext, [1] = sharedSecret
     */
    private external fun jniKemEncapsulate(recipientPublicKey: ByteArray): Array<ByteArray>

    /**
     * Native JNI function: ML-KEM Decapsulate
     * Returns shared secret bytes
     */
    private external fun jniKemDecapsulate(ciphertext: ByteArray, privateKey: ByteArray): ByteArray

    // ──── Public API for ML-KEM ──────────────────────────────────────────────

    /**
     * Generate ML-KEM keypair (NIST FIPS 203)
     * @return Pair containing (publicKey, privateKey)
     */
    fun generateMLKemKeypair(): Pair<ByteArray, ByteArray> {
        return try {
            val result = jniGenerateKemKeypair()
            Pair(result[0], result[1])
        } catch (e: Exception) {
            throw CryptoException("Failed to generate ML-KEM keypair", e)
        }
    }

    /**
     * Encapsulate (generate shared secret) using ML-KEM public key
     * @return KemEncapsulation containing ciphertext and sharedSecret
     */
    fun encapsulateMLKem(publicKey: ByteArray): KemEncapsulation {
        return try {
            val result = jniKemEncapsulate(publicKey)
            KemEncapsulation(result[0], result[1])
        } catch (e: Exception) {
            throw CryptoException("Failed to encapsulate with ML-KEM", e)
        }
    }

    /**
     * Decapsulate (recover shared secret) using ML-KEM private key
     */
    fun decapsulateMLKem(ciphertext: ByteArray, privateKey: ByteArray): ByteArray {
        return try {
            jniKemDecapsulate(ciphertext, privateKey)
        } catch (e: Exception) {
            throw CryptoException("Failed to decapsulate ML-KEM", e)
        }
    }

    // ──── Stub Implementation for ML-DSA (Future Enhancement) ─────────────────
    // ML-DSA implementation is planned but not yet included in liboqs binding.
    // These stubs allow code to compile while development continues.

    /**
     * Generate ML-DSA keypair (NIST FIPS 204)
     * ⚠️ STUB IMPLEMENTATION - Currently returns empty arrays
     * @return Pair containing publicKey and privateKey
     */
    fun generateMLDsaKeypair(): Pair<ByteArray, ByteArray> {
        // TODO: Implement ML-DSA support in liboqs or use Dilithium library
        return Pair(ByteArray(0), ByteArray(0))
    }

    /**
     * Sign data using ML-DSA private key
     * ⚠️ STUB IMPLEMENTATION
     */
    fun signWithMLDsa(message: ByteArray, privateKey: ByteArray): ByteArray {
        // TODO: Implement ML-DSA signature support
        return ByteArray(0)
    }

    /**
     * Verify signature using ML-DSA public key
     * ⚠️ STUB IMPLEMENTATION
     */
    fun verifyMLDsaSignature(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean {
        // TODO: Implement ML-DSA verification
        return false
    }
}

/**
 * Exception thrown during cryptographic operations
 */
class CryptoException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
