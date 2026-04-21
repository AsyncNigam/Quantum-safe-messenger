package com.nigdroid.quantummessenger.crypto

import java.security.SecureRandom

/**
 * Represents the state of a Double Ratchet session with one conversation partner.
 *
 * The Double Ratchet Algorithm provides:
 * 1. Forward secrecy: Deleting a message key reveals no information about past messages
 * 2. Break-in recovery: Compromise of current keys doesn't recover past messages
 * 3. Out-of-order tolerance: Messages can be decrypted later if arrived out of order
 */
data class DoubleRatchetSession(
    val userId: String,                    // ID of the remote user
    var sendChainKey: ByteArray,           // Key for deriving send keys (KDF chain)
    var receiveChainKey: ByteArray,        // Key for deriving receive keys
    var dhPublicKey: ByteArray,            // Our current Diffie-Hellman public key
    var dhPrivateKey: ByteArray,           // Our current Diffie-Hellman private key
    var remoteDhPublicKey: ByteArray,      // Remote user's last known DH public key
    var sendChainCounter: Int = 0,         // How many times we've ratcheted send
    var receiveChainCounter: Int = 0,      // How many times we've ratcheted receive
    var messageCounter: Int = 0,           // Counter within current chain
    var timestamp: Long = System.currentTimeMillis(),
    // Skipped message keys for out-of-order messages
    val skippedKeys: MutableMap<String, ByteArray> = mutableMapOf()
) {
    init {
        require(sendChainKey.isNotEmpty()) { "Send chain key cannot be empty" }
        require(receiveChainKey.isNotEmpty()) { "Receive chain key cannot be empty" }
        require(dhPublicKey.size == 32) { "DH public key must be 32 bytes (X25519)" }
        require(dhPrivateKey.size == 32) { "DH private key must be 32 bytes (X25519)" }
        require(remoteDhPublicKey.size == 32) { "Remote DH public key must be 32 bytes" }
    }

    /**
     * Generates a unique key identifier for a skipped message.
     * Format: "remoteChainCounter_messageCounter"
     */
    fun getSkippedKeyId(chainCounter: Int, msgCounter: Int): String {
        return "$chainCounter:$msgCounter"
    }

    /**
     * Cleanup old keys to prevent unbounded memory growth.
     * Keeps only the most recent 100 skipped keys.
     */
    fun pruneSkippedKeys(maxSize: Int = 100) {
        if (skippedKeys.size > maxSize) {
            val keysToDelete = skippedKeys.keys.drop(skippedKeys.size - maxSize)
            keysToDelete.forEach { skippedKeys.remove(it) }
        }
    }
}


/**
 * XChaCha20 DH result for ratcheting.
 */
data class DiffieHellmanResult(
    val publicKey: ByteArray,     // Our new public key
    val privateKey: ByteArray,    // Our new private key (KEEP SECURE)
    val sharedSecret: ByteArray   // DH result from (our private) * (their public)
) {
    init {
        require(publicKey.size == 32) { "Public key must be 32 bytes" }
        require(privateKey.size == 32) { "Private key must be 32 bytes" }
        require(sharedSecret.size == 32) { "Shared secret must be 32 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiffieHellmanResult) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (!sharedSecret.contentEquals(other.sharedSecret)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + sharedSecret.contentHashCode()
        return result
    }
}

/**
 * Message key with metadata for encryption/decryption.
 */
data class MessageKey(
    val key: ByteArray,           // The symmetric key for AES-GCM
    val chainCounter: Int,        // Which DH ratchet epoch
    val messageCounter: Int,      // Which message in this chain
    val initializationVector: ByteArray = ByteArray(12) // 96-bit IV for AES-GCM
) {
    init {
        require(key.size == 32) { "Message key must be 32 bytes" }
        require(initializationVector.size == 12) { "IV must be 96 bits (12 bytes)" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageKey) return false
        if (!key.contentEquals(other.key)) return false
        if (chainCounter != other.chainCounter) return false
        if (messageCounter != other.messageCounter) return false
        if (!initializationVector.contentEquals(other.initializationVector)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + chainCounter
        result = 31 * result + messageCounter
        result = 31 * result + initializationVector.contentHashCode()
        return result
    }
}

