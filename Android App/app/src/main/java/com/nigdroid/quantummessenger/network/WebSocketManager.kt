package com.nigdroid.quantummessenger.network

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.nigdroid.quantummessenger.proto.ChatMessage
import org.json.JSONObject

sealed class SocketEvent {
    data class MessageReceived(val message: ChatMessage) : SocketEvent()
    data object Connected : SocketEvent()
    data object Disconnected : SocketEvent()
    data class Error(val error: String) : SocketEvent()
}

object WebSocketManager {

    // Replace with your PC's local IP (run ipconfig in PowerShell)
    // Do NOT use localhost — it won't reach your PC from a physical phone
    private const val SERVER_URL = "http://192.168.x.x:3000"

    private var socket: Socket? = null

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events

    fun connect(jwtToken: String) {
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to jwtToken))  // matches socket.handshake.auth.token
            .build()

        socket = IO.socket(SERVER_URL, options)

        socket?.apply {

            on(Socket.EVENT_CONNECT) {
                _events.tryEmit(SocketEvent.Connected)
            }

            on(Socket.EVENT_DISCONNECT) {
                _events.tryEmit(SocketEvent.Disconnected)
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.getOrNull(0)?.toString() ?: "Connection error"
                _events.tryEmit(SocketEvent.Error(error))
            }

            // Incoming message from server → parse Protobuf bytes
            on("receive_message") { args ->
                try {
                    val bytes = args[0] as ByteArray
                    val message = ChatMessage.parseFrom(bytes)
                    _events.tryEmit(SocketEvent.MessageReceived(message))
                } catch (e: Exception) {
                    _events.tryEmit(SocketEvent.Error("Parse error: ${e.message}"))
                }
            }

            connect()
        }
    }

    fun sendMessage(message: ChatMessage) {
        // Serialize to Protobuf bytes and emit to server
        socket?.emit("send_message", message.toByteArray())
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
}