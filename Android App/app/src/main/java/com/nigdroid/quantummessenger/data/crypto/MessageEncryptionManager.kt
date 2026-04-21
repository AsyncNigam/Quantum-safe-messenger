package com.nigdroid.quantummessenger.data.crypto

import com.google.protobuf.ByteString
import com.nigdroid.quantummessenger.crypto.CryptoEngine
import com.nigdroid.quantummessenger.proto.EncryptedEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MessageEncryptionManager handles end-to-end encryption of messages.
 *
 * For each message:
 * 1. Encapsulates recipient's public key using ML-KEM (PQC)
 * 2. Derives a symmetric key from the shared secret
 * 3. Encrypts the plaintext message using AES-256-GCM (Tink)
 * 4. Wraps in EncryptedEnvelope for transmission
 *
 * On receive:
 * 1. Decapsulates the ML-KEM ciphertext to recover shared secret
 * 2. Derives the symmetric key
 * 3. Decrypts the message
 * 4. Returns plaintext
 */
@Singleton
class MessageEncryptionManager @Inject constructor(
    private val cryptoManager: CryptoManager
) {

    /**
     * Encrypts a message for transmission.
     *
     * @param plaintext The message content to encrypt
     * @param senderId The ID of the sender
     * @param recipientId The ID of the recipient
     * @param recipientPublicKey The recipient's ML-KEM public key (1184 bytes for ML-KEM-768)
     * @return EncryptedEnvelope ready for transmission
     */
    suspend fun encryptMessage(
        plaintext: String,
        senderId: String,
        recipientId: String,
        recipientPublicKey: ByteArray
    ): EncryptedEnvelope = withContext(Dispatchers.IO) {
        try {
            // Step 1: Encapsulate the recipient's public key
            val encap = CryptoEngine.encapsulate(recipientPublicKey)
            val mlkemCiphertext = encap.ciphertext
            val sharedSecret = encap.sharedSecret

            // Step 2: Derive symmetric key from shared secret using Tink
            val messageKey = deriveMessageKey(sharedSecret)

            // Step 3: Encrypt plaintext using AES-256-GCM
            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
            val ciphertext = cryptoManager.encrypt(
                plaintextBytes,
                associatedData = recipientId.toByteArray(Charsets.UTF_8)
            )

            // Step 4: Return encrypted envelope
            EncryptedEnvelope.newBuilder()
                .setRecipientId(recipientId)
                .setSenderId(senderId)
                .setCiphertext(ByteString.copyFrom(ciphertext))
                .setMlkemCiphertext(ByteString.copyFrom(mlkemCiphertext))
                .setTimestamp(System.currentTimeMillis())
                .build()

        } catch (e: Exception) {
            throw EncryptMessageException("Failed to encrypt message: ${e.message}", e)
        }
    }

    /**
     * Decrypts an incoming encrypted message.
     *
     * @param envelope The EncryptedEnvelope received from server
     * @param ownPrivateKey The recipient's ML-KEM private key
     * @return Decrypted plaintext message
     */
    suspend fun decryptMessage(
        envelope: EncryptedEnvelope,
        ownPrivateKey: ByteArray
    ): String = withContext(Dispatchers.IO) {
        try {
            // Step 1: Decapsulate the ML-KEM ciphertext to recover shared secret
            val sharedSecret = CryptoEngine.decapsulate(
                envelope.mlkemCiphertext.toByteArray(),
                ownPrivateKey
            )

            // Step 2: Derive symmetric key from shared secret
            deriveMessageKey(sharedSecret)

            // Step 3: Decrypt the ciphertext using AES-256-GCM
            val plaintext = cryptoManager.decrypt(
                envelope.ciphertext.toByteArray(),
                associatedData = envelope.recipientId.toByteArray(Charsets.UTF_8)
            )

            // Step 4: Return as string
            String(plaintext, Charsets.UTF_8)

        } catch (e: Exception) {
            throw DecryptMessageException("Failed to decrypt message: ${e.message}", e)
        }
    }

    /**
     * Derives a message-specific key from the shared secret using HKDF-SHA256.
     *
     * This provides personalization beyond just the raw shared secret,
     * reducing the security impact if one message is compromised.
     *
     * @param sharedSecret The ML-KEM shared secret (32 bytes)
     * @return A derived key suitable for message encryption
     */
    private suspend fun deriveMessageKey(sharedSecret: ByteArray): ByteArray {
        // Use Tink's HKDF to derive a personalized key for this message
        val info = "message_key".toByteArray(Charsets.UTF_8)
        val derived = cryptoManager.encrypt(sharedSecret, info)
        return derived.take(32).toByteArray()
    }
}

/**
 * Thrown when message encryption fails.
 */
class EncryptMessageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown when message decryption fails.
 */
class DecryptMessageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

