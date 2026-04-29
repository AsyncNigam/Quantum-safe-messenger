package com.nigdroid.quantummessenger.crypto

import java.security.SecureRandom

data class DoubleRatchetSession(
    val userId: String,
    var sendChainKey: ByteArray,
    var receiveChainKey: ByteArray,
    var dhPublicKey: ByteArray,
    var dhPrivateKey: ByteArray,
    var remoteDhPublicKey: ByteArray,
    var sendChainCounter: Int = 0,
    var receiveChainCounter: Int = 0,
    var messageCounter: Int = 0,
    var timestamp: Long = System.currentTimeMillis(),
    val skippedKeys: MutableMap<String, ByteArray> = mutableMapOf()
) {
    init {
        require(sendChainKey.isNotEmpty()) { "Send chain key cannot be empty" }
        require(receiveChainKey.isNotEmpty()) { "Receive chain key cannot be empty" }
        require(dhPublicKey.size == 32) { "DH public key must be 32 bytes (X25519)" }
        require(dhPrivateKey.size == 32) { "DH private key must be 32 bytes (X25519)" }
        require(remoteDhPublicKey.size == 32) { "Remote DH public key must be 32 bytes" }
    }

    fun getSkippedKeyId(chainCounter: Int, msgCounter: Int): String {
        return "$chainCounter:$msgCounter"
    }

    fun pruneSkippedKeys(maxSize: Int = 100) {
        if (skippedKeys.size > maxSize) {
            val keysToDelete = skippedKeys.keys.drop(skippedKeys.size - maxSize)
            keysToDelete.forEach { skippedKeys.remove(it) }
        }
    }
}

data class DiffieHellmanResult(
    val publicKey: ByteArray,
    val privateKey: ByteArray,
    val sharedSecret: ByteArray
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

data class MessageKey(
    val key: ByteArray,
    val chainCounter: Int,
    val messageCounter: Int,
    val initializationVector: ByteArray = ByteArray(12)
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
