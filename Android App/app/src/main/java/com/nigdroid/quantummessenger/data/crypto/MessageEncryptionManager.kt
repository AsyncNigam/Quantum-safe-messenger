package com.nigdroid.quantummessenger.data.crypto

import com.google.protobuf.ByteString
import com.nigdroid.quantummessenger.crypto.CryptoEngine
import com.nigdroid.quantummessenger.proto.EncryptedEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageEncryptionManager @Inject constructor(
    private val cryptoManager: CryptoManager
) {

    suspend fun encryptMessage(
        plaintext: String,
        senderId: String,
        recipientId: String,
        recipientPublicKey: ByteArray
    ): EncryptedEnvelope = withContext(Dispatchers.IO) {
        try {
            val encap = CryptoEngine.encapsulate(recipientPublicKey)
            val messageKey = deriveMessageKey(encap.sharedSecret)

            val ciphertext = cryptoManager.encrypt(
                plaintext.toByteArray(Charsets.UTF_8),
                associatedData = recipientId.toByteArray(Charsets.UTF_8)
            )

            EncryptedEnvelope.newBuilder()
                .setRecipientId(recipientId)
                .setSenderId(senderId)
                .setCiphertext(ByteString.copyFrom(ciphertext))
                .setMlkemCiphertext(ByteString.copyFrom(encap.ciphertext))
                .setTimestamp(System.currentTimeMillis())
                .build()
        } catch (e: Exception) {
            throw EncryptMessageException("Failed to encrypt message: ${e.message}", e)
        }
    }

    suspend fun decryptMessage(
        envelope: EncryptedEnvelope,
        ownPrivateKey: ByteArray
    ): String = withContext(Dispatchers.IO) {
        try {
            val sharedSecret = CryptoEngine.decapsulate(
                envelope.mlkemCiphertext.toByteArray(),
                ownPrivateKey
            )
            deriveMessageKey(sharedSecret)

            val plaintext = cryptoManager.decrypt(
                envelope.ciphertext.toByteArray(),
                associatedData = envelope.recipientId.toByteArray(Charsets.UTF_8)
            )

            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            throw DecryptMessageException("Failed to decrypt message: ${e.message}", e)
        }
    }

    private suspend fun deriveMessageKey(sharedSecret: ByteArray): ByteArray {
        val info = "message_key".toByteArray(Charsets.UTF_8)
        val derived = cryptoManager.encrypt(sharedSecret, info)
        return derived.take(32).toByteArray()
    }
}

class EncryptMessageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class DecryptMessageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
