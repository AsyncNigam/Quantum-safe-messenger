package com.nigdroid.quantummessenger.domain.model

/**
 * Domain models for Zero-Knowledge anonymous authentication.
 * No PII (email, phone, name) is stored anywhere in this model.
 */

/**
 * Generated cryptographic identity.
 * All private keys are stored securely in Android Keystore.
 * Only public key material and the derived fingerprint are kept here.
 */
data class Identity(
    /** SHA-256 fingerprint — the user's permanent anonymous identity */
    val textFingerprint: String,
    val mlKemPublicKey: ByteArray,      // ML-KEM-768 public key
    val x25519PublicKey: ByteArray,     // X25519 DH key
    val createdAt: Long = System.currentTimeMillis(),
    val keyStoreAliasPrefix: String = "quantum_messenger_"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Identity) return false
        return textFingerprint == other.textFingerprint
    }

    override fun hashCode(): Int = textFingerprint.hashCode()
}

/**
 * Request payload sent to POST /auth/register
 */
data class AuthRegisterRequest(
    val mlKemPublicKey: String,   // Base64-encoded
    val x25519PublicKey: String   // Base64-encoded
)

/**
 * Response from POST /auth/register
 */
data class AuthRegisterResponse(
    val success: Boolean,
    val textFingerprint: String
)

/**
 * In-memory keypair (discarded after Keystore storage)
 */
data class KeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyPair) return false
        return publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int = publicKey.contentHashCode()

    /** Securely zero out sensitive data */
    fun clear() { privateKey?.fill(0) }
}

/**
 * Result of the full identity generation + registration workflow
 */
sealed class IdentityGenerationResult {
    data class Success(val identity: Identity) : IdentityGenerationResult()
    data class Error(val exception: Exception, val message: String) : IdentityGenerationResult()
    object Cancelled : IdentityGenerationResult()
}

/**
 * Result of registering identity with the backend
 */
sealed class AuthenticationResult {
    data class Success(val textFingerprint: String, val identity: Identity) : AuthenticationResult()
    data class Error(val exception: Exception, val message: String) : AuthenticationResult()
    object NetworkError : AuthenticationResult()
}
