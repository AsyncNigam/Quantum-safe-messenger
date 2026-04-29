package com.nigdroid.quantummessenger.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoubleRatchetManager @Inject constructor() {

    private val secureRandom = SecureRandom()
    private val sessions = mutableMapOf<String, DoubleRatchetSession>()

    suspend fun initializeSession(
        userId: String,
        sharedSecret: ByteArray,
        remotePublicKey: ByteArray,
        ourKeyPair: DiffieHellmanResult
    ): DoubleRatchetSession = withContext(Dispatchers.Default) {
        try {
            val (sendChainKey, receiveChainKey) = deriveInitialChainKeys(sharedSecret)

            val session = DoubleRatchetSession(
                userId = userId,
                sendChainKey = sendChainKey,
                receiveChainKey = receiveChainKey,
                dhPublicKey = ourKeyPair.publicKey,
                dhPrivateKey = ourKeyPair.privateKey,
                remoteDhPublicKey = remotePublicKey,
                sendChainCounter = 0,
                receiveChainCounter = 0,
                messageCounter = 0
            )

            sessions[userId] = session
            session
        } catch (e: Exception) {
            throw RatchetException("Failed to initialize session with $userId: ${e.message}", e)
        }
    }

    fun getSession(userId: String): DoubleRatchetSession {
        return sessions[userId] ?: throw RatchetException("No session found for $userId")
    }

    suspend fun getNextMessageKey(session: DoubleRatchetSession): MessageKey =
        withContext(Dispatchers.Default) {
            try {
                val messageKey = deriveMessageKey(session.sendChainKey)
                session.sendChainKey = advanceChainKey(session.sendChainKey)
                session.messageCounter++

                val iv = ByteArray(12)
                secureRandom.nextBytes(iv)

                MessageKey(
                    key = messageKey,
                    chainCounter = session.sendChainCounter,
                    messageCounter = session.messageCounter,
                    initializationVector = iv
                )
            } catch (e: Exception) {
                throw RatchetException("Failed to derive message key: ${e.message}", e)
            }
        }

    suspend fun performDhRatchet(
        session: DoubleRatchetSession,
        newDhResult: DiffieHellmanResult
    ) = withContext(Dispatchers.Default) {
        try {
            val dhSecret = computeSharedSecret(newDhResult.privateKey, session.remoteDhPublicKey)
            session.sendChainKey = deriveChainKeyFromDh(dhSecret)
            session.sendChainCounter++
            session.messageCounter = 0
            session.dhPublicKey = newDhResult.publicKey
            session.dhPrivateKey = newDhResult.privateKey
        } catch (e: Exception) {
            throw RatchetException("Failed to perform DH ratchet: ${e.message}", e)
        }
    }

    suspend fun updateRemoteDhKey(
        session: DoubleRatchetSession,
        remoteDhPublicKey: ByteArray
    ) = withContext(Dispatchers.Default) {
        try {
            if (!remoteDhPublicKey.contentEquals(session.remoteDhPublicKey)) {
                val dhSecret = computeSharedSecret(session.dhPrivateKey, remoteDhPublicKey)
                session.receiveChainKey = deriveChainKeyFromDh(dhSecret)
                session.receiveChainCounter++
                session.messageCounter = 0
                session.remoteDhPublicKey = remoteDhPublicKey
            }
        } catch (e: Exception) {
            throw RatchetException("Failed to update remote DH key: ${e.message}", e)
        }
    }

    suspend fun getMessageKey(
        session: DoubleRatchetSession,
        remoteChainCounter: Int,
        remoteMessageCounter: Int
    ): MessageKey = withContext(Dispatchers.Default) {
        try {
            val keyId = session.getSkippedKeyId(remoteChainCounter, remoteMessageCounter)
            val skippedKey = session.skippedKeys.remove(keyId)
            if (skippedKey != null) {
                val iv = ByteArray(12)
                secureRandom.nextBytes(iv)
                return@withContext MessageKey(skippedKey, remoteChainCounter, remoteMessageCounter, iv)
            }

            if (remoteChainCounter == session.receiveChainCounter) {
                while (session.messageCounter < remoteMessageCounter) {
                    val skippedMsg = deriveMessageKey(session.receiveChainKey)
                    val skippedId = session.getSkippedKeyId(remoteChainCounter, session.messageCounter)
                    session.skippedKeys[skippedId] = skippedMsg
                    session.receiveChainKey = advanceChainKey(session.receiveChainKey)
                    session.messageCounter++
                }

                val messageKey = deriveMessageKey(session.receiveChainKey)
                session.receiveChainKey = advanceChainKey(session.receiveChainKey)
                session.messageCounter++

                val iv = ByteArray(12)
                secureRandom.nextBytes(iv)
                MessageKey(messageKey, remoteChainCounter, remoteMessageCounter, iv)
            } else {
                throw RatchetException(
                    "Message from different chain epoch (expected ${session.receiveChainCounter}, got $remoteChainCounter)"
                )
            }
        } catch (e: RatchetException) {
            throw e
        } catch (e: Exception) {
            throw RatchetException("Failed to get message key: ${e.message}", e)
        }
    }

    fun clearSession(userId: String) {
        sessions.remove(userId)?.let { session ->
            session.sendChainKey.fill(0)
            session.receiveChainKey.fill(0)
            session.dhPrivateKey.fill(0)
            session.skippedKeys.values.forEach { it.fill(0) }
            session.skippedKeys.clear()
        }
    }

    private fun deriveInitialChainKeys(sharedSecret: ByteArray): Pair<ByteArray, ByteArray> {
        val sendKey = kdf(sharedSecret, "send_chain_key").take(32).toByteArray()
        val receiveKey = kdf(sharedSecret, "receive_chain_key").take(32).toByteArray()
        return Pair(sendKey, receiveKey)
    }

    private fun advanceChainKey(chainKey: ByteArray): ByteArray {
        return kdf(chainKey, "chain_advance").take(32).toByteArray()
    }

    private fun deriveMessageKey(chainKey: ByteArray): ByteArray {
        return kdf(chainKey, "message_key").take(32).toByteArray()
    }

    private fun deriveChainKeyFromDh(dhSecret: ByteArray): ByteArray {
        return kdf(dhSecret, "chain_from_dh").take(32).toByteArray()
    }

    private fun kdf(ikm: ByteArray, info: String): ByteArray {
        val key = SecretKeySpec(ikm, 0, ikm.size, "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        mac.update(info.toByteArray(Charsets.UTF_8))
        return mac.doFinal()
    }

    private fun computeSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        // Placeholder: in production, use Conscrypt or alternative X25519
        return (privateKey + publicKey).take(32).toByteArray()
    }
}

class RatchetException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
