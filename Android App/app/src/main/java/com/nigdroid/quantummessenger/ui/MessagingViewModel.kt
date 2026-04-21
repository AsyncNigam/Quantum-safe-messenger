package com.nigdroid.quantummessenger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.crypto.CryptoEngine
import com.nigdroid.quantummessenger.crypto.KemKeypair
import com.nigdroid.quantummessenger.crypto.DoubleRatchetManager
import com.nigdroid.quantummessenger.crypto.DiffieHellmanResult
import com.nigdroid.quantummessenger.data.crypto.MessageEncryptionManager
import com.nigdroid.quantummessenger.network.SocketEvent
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.proto.ChatMessage
import com.nigdroid.quantummessenger.proto.EncryptedEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

// ── UI state ──────────────────────────────────────────────────────────────────
data class MessageUiModel(
    val senderId: String,
    val plaintext: String,       // decrypted text ready to display
    val timestamp: Long,
    val isEncrypted: Boolean = true
)

data class MessagingUiState(
    val connectionStatus: String = "Disconnected",
    val messages: List<MessageUiModel> = emptyList(),
    val error: String? = null,
    val ownPublicKeyHex: String = "",  // shown in UI for debugging
    val encryptionStatus: String = "Initializing..."
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class MessagingViewModel @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val messageEncryptionManager: MessageEncryptionManager,
    private val doubleRatchetManager: DoubleRatchetManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    // Our own keypair — generated once per session
    // In production this will be persisted in Android Keystore
    private var ownKeypair: KemKeypair? = null
    private var dhKeyPair: DiffieHellmanResult? = null

    // Current conversation partner
    private var currentPartnerId: String? = null
    private var partnerPublicKey: ByteArray? = null

    // Shared secrets keyed by sender ID (Phase 5)
    // In Phase 6+ this becomes the Double Ratchet state store
    private val sharedSecrets = mutableMapOf<String, ByteArray>()

    init {
        generateOwnKeypair()
        observeSocketEvents()
    }

    // ── Key generation ────────────────────────────────────────────────────────
    private fun generateOwnKeypair() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val keypair = CryptoEngine.generateKeypair()
                ownKeypair = keypair

                // Generate X25519 DH keypair for ratcheting
                val dh = generateDhKeypair()
                dhKeyPair = dh

                val hexPreview = keypair.publicKey
                    .take(16)
                    .joinToString("") { "%02x".format(it) }

                _uiState.value = _uiState.value.copy(
                    ownPublicKeyHex = hexPreview,
                    encryptionStatus = "Ready"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Key generation failed: ${e.message}",
                    encryptionStatus = "Error"
                )
            }
        }
    }

    /**
     * Generate X25519 DH keypair (placeholder).
     * In production, integrate with actual X25519 library.
     */
    private fun generateDhKeypair(): DiffieHellmanResult {
        val random = SecureRandom()
        val privateKey = ByteArray(32)
        val publicKey = ByteArray(32)
        random.nextBytes(privateKey)
        random.nextBytes(publicKey)
        val sharedSecret = ByteArray(32)
        random.nextBytes(sharedSecret)
        return DiffieHellmanResult(publicKey, privateKey, sharedSecret)
    }

    // ── Connect to backend ────────────────────────────────────────────────────
    fun connect(jwtToken: String) {
        webSocketManager.connect(jwtToken)
    }

    fun setPartner(partnerId: String, partnerPublicKey: ByteArray) {
        currentPartnerId = partnerId
        this.partnerPublicKey = partnerPublicKey
    }

    // ── Observe all socket events ─────────────────────────────────────────────
    private fun observeSocketEvents() {
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event) {

                    is SocketEvent.Connected -> {
                        _uiState.value = _uiState.value.copy(
                            connectionStatus = "Connected",
                            error = null
                        )
                    }

                    is SocketEvent.Disconnected -> {
                        _uiState.value = _uiState.value.copy(
                            connectionStatus = "Disconnected"
                        )
                    }

                    is SocketEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = event.error
                        )
                    }

                    is SocketEvent.MessageReceived -> {
                        handleIncomingMessage(event.message)
                    }

                    is SocketEvent.EncryptedMessageReceived -> {
                        handleIncomingEncryptedMessage(event.envelope)
                    }
                }
            }
        }
    }

    // ── Handle Phase 5: Encrypted message without ratcheting ─────────────────
    private fun handleIncomingMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val keypair = ownKeypair ?: return@launch

                val payloadBytes = message.payload.toByteArray()
                val kemCtSize = 1088  // ML-KEM-768 ciphertext length

                if (payloadBytes.size < kemCtSize) {
                    appendMessage(
                        MessageUiModel(
                            senderId = message.senderId,
                            plaintext = "[payload too short to decapsulate]",
                            timestamp = message.timestamp,
                            isEncrypted = false
                        )
                    )
                    return@launch
                }

                val kemCiphertext = payloadBytes.sliceArray(0 until kemCtSize)
                val sharedSecret = CryptoEngine.decapsulate(kemCiphertext, keypair.privateKey)

                sharedSecrets[message.senderId] = sharedSecret

                val plaintext = "[decrypted — shared secret: " +
                        sharedSecret.take(8).joinToString("") { "%02x".format(it) } + "...]"

                appendMessage(
                    MessageUiModel(
                        senderId = message.senderId,
                        plaintext = plaintext,
                        timestamp = message.timestamp,
                        isEncrypted = true
                    )
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Decryption failed: ${e.message}"
                )
            }
        }
    }

    // ── Handle Phase 5+6: Encrypted envelope with PQC-encapsulated key ──────
    private fun handleIncomingEncryptedMessage(envelope: EncryptedEnvelope) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val keypair = ownKeypair ?: return@launch

                // Step 1: Decapsulate ML-KEM ciphertext
                val sharedSecret = CryptoEngine.decapsulate(
                    envelope.mlkemCiphertext.toByteArray(),
                    keypair.privateKey
                )
                sharedSecrets[envelope.senderId] = sharedSecret

                // Step 2: Decrypt using Tink
                val plaintext = messageEncryptionManager.decryptMessage(
                    envelope,
                    keypair.privateKey
                )

                appendMessage(
                    MessageUiModel(
                        senderId = envelope.senderId,
                        plaintext = plaintext,
                        timestamp = envelope.timestamp,
                        isEncrypted = true
                    )
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "E2E decryption failed: ${e.message}"
                )
            }
        }
    }

    // ── Send an encrypted message ─────────────────────────────────────────────
    fun sendMessage(
        recipientId: String,
        recipientPublicKey: ByteArray,
        plaintextContent: String
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val endpoint = currentPartnerId ?: recipientId
                val pubKey = partnerPublicKey ?: recipientPublicKey

                // Step 1: Encrypt message using Tink + ML-KEM
                val envelope = messageEncryptionManager.encryptMessage(
                    plaintext = plaintextContent,
                    senderId = "self",  // Phase 7: use real user ID from auth
                    recipientId = endpoint,
                    recipientPublicKey = pubKey
                )

                // Step 2: Send via WebSocket
                webSocketManager.sendEncryptedMessage(envelope)

                // Step 3: Show in UI
                appendMessage(
                    MessageUiModel(
                        senderId = "self",
                        plaintext = plaintextContent,
                        timestamp = System.currentTimeMillis(),
                        isEncrypted = true
                    )
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Send error: ${e.message}"
                )
            }
        }
    }

    private fun appendMessage(msg: MessageUiModel) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + msg
        )
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
        // Clear sensitive data
        ownKeypair?.privateKey?.fill(0)
        dhKeyPair?.privateKey?.fill(0)
        partnerPublicKey?.fill(0)
        sharedSecrets.values.forEach { it.fill(0) }
        sharedSecrets.clear()
    }
}
