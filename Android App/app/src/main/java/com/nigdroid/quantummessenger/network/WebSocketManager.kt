package com.nigdroid.quantummessenger.network

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.nigdroid.quantummessenger.proto.ChatMessage
import com.nigdroid.quantummessenger.proto.EncryptedEnvelope
import org.json.JSONObject

import javax.inject.Inject
import javax.inject.Singleton

sealed class SocketEvent {
    data class MessageReceived(val message: ChatMessage) : SocketEvent()
    data class EncryptedMessageReceived(val envelope: EncryptedEnvelope) : SocketEvent()
    data object Connected : SocketEvent()
    data object Disconnected : SocketEvent()
    data class Error(val error: String) : SocketEvent()
}

@Singleton
class WebSocketManager @Inject constructor() {

    // Replace with your PC's local IP (run ipconfig in PowerShell)
    // Do NOT use localhost — it won't reach your PC from a physical phone
    private val SERVER_URL = "http://192.168.x.x:3000"

    private var socket: Socket? = null

    // Connection state for UI to display "Connected" or "Disconnected" indicator
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    fun connect(jwtToken: String) {
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to jwtToken))  // matches socket.handshake.auth.token
            .build()

        socket = IO.socket(SERVER_URL, options)

        socket?.apply {

            on(Socket.EVENT_CONNECT) {
                _connectionState.value = true
                _events.tryEmit(SocketEvent.Connected)
            }

            on(Socket.EVENT_DISCONNECT) {
                _connectionState.value = false
                _events.tryEmit(SocketEvent.Disconnected)
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                _connectionState.value = false
                val error = args.getOrNull(0)?.toString() ?: "Connection error"
                _events.tryEmit(SocketEvent.Error(error))
            }

            // Incoming message from server → parse Protobuf bytes (legacy)
            on("receive_message") { args ->
                try {
                    val bytes = args[0] as ByteArray
                    val message = ChatMessage.parseFrom(bytes)
                    _events.tryEmit(SocketEvent.MessageReceived(message))
                } catch (e: Exception) {
                    _events.tryEmit(SocketEvent.Error("Parse error: ${e.message}"))
                }
            }

            // Incoming encrypted message with PQC key encapsulation
            on("receive_encrypted_message") { args ->
                try {
                    val bytes = args[0] as ByteArray
                    val envelope = EncryptedEnvelope.parseFrom(bytes)
                    _events.tryEmit(SocketEvent.EncryptedMessageReceived(envelope))
                } catch (e: Exception) {
                    _events.tryEmit(SocketEvent.Error("Encrypted message parse error: ${e.message}"))
                }
            }

            connect()
        }
    }

    fun sendMessage(message: ChatMessage) {
        // Serialize to Protobuf bytes and emit to server (legacy)
        socket?.emit("send_message", message.toByteArray())
    }

    fun sendEncryptedMessage(envelope: EncryptedEnvelope) {
        // Send encrypted envelope with PQC-encapsulated key
        socket?.emit("send_encrypted_message", envelope.toByteArray())
    }

    fun disconnect() {
        _connectionState.value = false
        socket?.disconnect()
        socket = null
    }

    fun isConnected(): Boolean = socket?.connected() ?: false
}