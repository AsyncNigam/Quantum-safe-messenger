package com.nigdroid.quantummessenger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nigdroid.quantummessenger.crypto.CryptoEngine
import com.nigdroid.quantummessenger.crypto.KemKeypair
import com.nigdroid.quantummessenger.network.SocketEvent
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.proto.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI state ──────────────────────────────────────────────────────────────────
data class MessageUiModel(
    val senderId: String,
    val plaintext: String,       // decrypted text ready to display
    val timestamp: Long
)

data class MessagingUiState(
    val connectionStatus: String = "Disconnected",
    val messages: List<MessageUiModel> = emptyList(),
    val error: String? = null,
    val ownPublicKeyHex: String = ""  // shown in UI for debugging
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class MessagingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    // Our own keypair — generated once per session
    // In a real build this will be persisted in Android Keystore
    private var ownKeypair: KemKeypair? = null

    // Shared secrets keyed by sender ID
    // In Phase 5 this becomes the Double Ratchet state store
    private val sharedSecrets = mutableMapOf<String, ByteArray>()

    init {
        generateOwnKeypair()
        observeSocketEvents()
    }

    // ── Key generation ────────────────────────────────────────────────────────
    private fun generateOwnKeypair() {
        viewModelScope.launch(Dispatchers.Default) {
            val keypair = CryptoEngine.generateKeypair()
            ownKeypair = keypair

            // Show first 16 bytes of public key as hex in UI (debug only)
            val hexPreview = keypair.publicKey
                .take(16)
                .joinToString("") { "%02x".format(it) }

            _uiState.value = _uiState.value.copy(
                ownPublicKeyHex = hexPreview
            )
        }
    }

    // ── Connect to backend ────────────────────────────────────────────────────
    fun connect(jwtToken: String) {
        WebSocketManager.connect(jwtToken)
    }

    // ── Observe all socket events ─────────────────────────────────────────────
    private fun observeSocketEvents() {
        viewModelScope.launch {
            WebSocketManager.events.collect { event ->
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
                }
            }
        }
    }

    // ── Handle incoming encrypted message ─────────────────────────────────────
    private fun handleIncomingMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val keypair = ownKeypair ?: return@launch

                // payload bytes = KEM ciphertext (first 1088 bytes for ML-KEM-768)
                //                 + AES-GCM ciphertext (rest)
                // Phase 5 will split this properly with Tink
                // For now we just decapsulate to prove the shared secret arrives
                val payloadBytes = message.payload.toByteArray()

                val kemCtSize = 1088  // ML-KEM-768 ciphertext length
                if (payloadBytes.size < kemCtSize) {
                    appendMessage(
                        MessageUiModel(
                            senderId  = message.senderId,
                            plaintext = "[payload too short to decapsulate]",
                            timestamp = message.timestamp
                        )
                    )
                    return@launch
                }

                val kemCiphertext = payloadBytes.sliceArray(0 until kemCtSize)
                val sharedSecret  = CryptoEngine.decapsulate(kemCiphertext, keypair.privateKey)

                // Cache shared secret — Phase 5 feeds this into Double Ratchet
                sharedSecrets[message.senderId] = sharedSecret

                // Plaintext stub — Phase 5 replaces this with Tink AES-GCM decrypt
                val plaintext = "[encrypted — shared secret established: " +
                        sharedSecret.take(8).joinToString("") { "%02x".format(it) } + "...]"

                appendMessage(
                    MessageUiModel(
                        senderId  = message.senderId,
                        plaintext = plaintext,
                        timestamp = message.timestamp
                    )
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Decrypt error: ${e.message}"
                )
            }
        }
    }

    // ── Send an encrypted message ─────────────────────────────────────────────
    fun sendMessage(
        recipientId: String,
        recipientPublicKey: ByteArray,
        plaintextBytes: ByteArray
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Encapsulate → get ciphertext + shared secret
                val encap = CryptoEngine.encapsulate(recipientPublicKey)

                // Phase 5: use sharedSecret with Tink to encrypt plaintextBytes
                // For now we send kemCiphertext + plaintext concatenated as stub
                val payload = encap.ciphertext + plaintextBytes

                val proto = ChatMessage.newBuilder()
                    .setSenderId("self")           // Phase 5: real user ID from Supabase auth
                    .setRecipientId(recipientId)
                    .setPayload(com.google.protobuf.ByteString.copyFrom(payload))
                    .setTimestamp(System.currentTimeMillis())
                    .build()

                WebSocketManager.sendMessage(proto)

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
        WebSocketManager.disconnect()
    }
}
