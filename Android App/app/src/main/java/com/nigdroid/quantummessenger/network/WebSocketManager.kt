package com.nigdroid.quantummessenger.network

import com.nigdroid.quantummessenger.util.Constants
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.nigdroid.quantummessenger.proto.ChatMessage as ProtoMessage
import com.nigdroid.quantummessenger.proto.EncryptedEnvelope
import org.json.JSONObject

import javax.inject.Inject
import javax.inject.Singleton

sealed class SocketEvent {
    data class MessageReceived(val message: ProtoMessage) : SocketEvent()
    data class EncryptedMessageReceived(val envelope: EncryptedEnvelope) : SocketEvent()
    data object Connected : SocketEvent()
    data object Disconnected : SocketEvent()
    data class Error(val error: String) : SocketEvent()
}

@Singleton
class WebSocketManager @Inject constructor() {

    private val SERVER_URL = Constants.WEBSOCKET_URL

    private var socket: Socket? = null
    private var currentFingerprint: String? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    fun connect(fingerprint: String) {
        // If already connected with same fingerprint, skip
        if (socket?.connected() == true && currentFingerprint == fingerprint) return

        // Disconnect existing socket if any
        socket?.off()
        socket?.disconnect()

        currentFingerprint = fingerprint

        val options = IO.Options.builder()
            .setAuth(mapOf("fingerprint" to fingerprint))
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1000)
            .setReconnectionDelayMax(5000)
            .setTimeout(10000)
            .build()

        socket = IO.socket(SERVER_URL, options)

        socket?.apply {

            on(Socket.EVENT_CONNECT) {
                _connectionState.value = true
                _events.tryEmit(SocketEvent.Connected)
                android.util.Log.d("WebSocket", "Connected to server")
            }

            on(Socket.EVENT_DISCONNECT) {
                _connectionState.value = false
                _events.tryEmit(SocketEvent.Disconnected)
                android.util.Log.d("WebSocket", "Disconnected from server")
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                _connectionState.value = false
                val error = args.getOrNull(0)?.toString() ?: "Connection error"
                _events.tryEmit(SocketEvent.Error(error))
                android.util.Log.e("WebSocket", "Connection error: $error")
            }

            on("receive_message") { args ->
                try {
                    val raw = args[0]
                    when (raw) {
                        is ByteArray -> {
                            val message = ProtoMessage.parseFrom(raw)
                            _events.tryEmit(SocketEvent.MessageReceived(message))
                        }
                        is JSONObject -> {
                            val payloadB64 = raw.getString("payload")
                            val payloadBytes = android.util.Base64.decode(payloadB64, android.util.Base64.NO_WRAP)
                            val message = ProtoMessage.parseFrom(payloadBytes)
                            _events.tryEmit(SocketEvent.MessageReceived(message))
                        }
                        is String -> {
                            val json = JSONObject(raw)
                            val payloadB64 = json.getString("payload")
                            val payloadBytes = android.util.Base64.decode(payloadB64, android.util.Base64.NO_WRAP)
                            val message = ProtoMessage.parseFrom(payloadBytes)
                            _events.tryEmit(SocketEvent.MessageReceived(message))
                        }
                        else -> {
                            _events.tryEmit(SocketEvent.Error("Unknown message format: ${raw?.javaClass?.name}"))
                        }
                    }
                } catch (e: Exception) {
                    _events.tryEmit(SocketEvent.Error("Parse error: ${e.message}"))
                }
            }

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

    fun sendMessage(message: ProtoMessage) {
        val json = JSONObject().apply {
            put("to", message.recipientId)
            put("payload", android.util.Base64.encodeToString(
                message.toByteArray(), android.util.Base64.NO_WRAP
            ))
        }
        socket?.emit("send_message", json)
    }

    fun sendEncryptedMessage(envelope: EncryptedEnvelope) {
        socket?.emit("send_encrypted_message", envelope.toByteArray())
    }

    fun disconnect() {
        _connectionState.value = false
        currentFingerprint = null
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    fun isConnected(): Boolean = socket?.connected() ?: false

    fun ensureConnected(fingerprint: String) {
        if (!isConnected()) {
            connect(fingerprint)
        }
    }
}
