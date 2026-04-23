package com.nigdroid.quantummessenger.domain.model

/**
 * Domain models for authentication
 */

/**
 * Generated identity containing cryptographic key material.
 * All private keys are stored securely in Android Keystore.
 * This model only contains references and public key material.
 */
data class Identity(
    val userId: String,
    val identifier: String,             // Email or Phone Number
    val mlKemPublicKey: ByteArray,      // ML-KEM public key (bytes)
    val mlDsaPublicKey: ByteArray,      // ML-DSA signature verification key (bytes)
    val x25519PublicKey: ByteArray,     // X25519 ephemeral key (bytes)
    val ed25519PublicKey: ByteArray,    // Ed25519 signature key (bytes)
    val createdAt: Long = System.currentTimeMillis(),
    val keyStoreAliasPrefix: String = "quantum_messenger_"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Identity) return false
        return userId == other.userId && identifier == other.identifier
    }

    override fun hashCode(): Int {
        return 31 * userId.hashCode() + identifier.hashCode()
    }
}

/**
 * Request payload for authentication server registration
 */
data class AuthRegisterRequest(
    val identifier: String,             // Email or Phone Number
    val mlKemPublicKey: String,         // Base64 encoded
    val mlDsaPublicKey: String,         // Base64 encoded
    val x25519PublicKey: String,        // Base64 encoded
    val ed25519PublicKey: String,       // Base64 encoded
    val deviceToken: String = "",       // Optional FCM token
    val deviceName: String = ""         // Device identifier
)

/**
 * Response from authentication server
 */
data class AuthRegisterResponse(
    val success: Boolean,
    val userId: String,
    val message: String,
    val serverPublicKey: String?,       // Base64 encoded server's X25519 public key
    val challengeNonce: String?         // For proof-of-identity
)

/**
 * Represents generated keypair (in-memory, for processing only)
 */
data class KeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray? = null   // Should be null after storing in Keystore
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyPair) return false
        return publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int {
        return publicKey.contentHashCode()
    }

    /**
     * Securely clear sensitive data
     */
    fun clear() {
        privateKey?.fill(0)
    }
}

/**
 * Result of identity generation workflow
 */
sealed class IdentityGenerationResult {
    data class Success(val identity: Identity) : IdentityGenerationResult()
    data class Error(val exception: Exception, val message: String) : IdentityGenerationResult()
    object Cancelled : IdentityGenerationResult()
}

/**
 * Result of authentication registration
 */
sealed class AuthenticationResult {
    data class Success(val userId: String, val identity: Identity) : AuthenticationResult()
    data class Error(val exception: Exception, val message: String) : AuthenticationResult()
    object NetworkError : AuthenticationResult()
    object InvalidInput : AuthenticationResult()
}
