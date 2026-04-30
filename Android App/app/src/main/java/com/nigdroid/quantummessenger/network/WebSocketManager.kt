package com.nigdroid.quantummessenger.network

import com.nigdroid.quantummessenger.util.Constants
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import com.nigdroid.quantummessenger.proto.ChatMessage as ProtoMessage
import org.json.JSONObject

import javax.inject.Inject
import javax.inject.Singleton

sealed class SocketEvent {
    data class UserDeleted(val fingerprint: String) : SocketEvent()
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

    // Channel.UNLIMITED guarantees NO messages are ever dropped.
    // Unlike SharedFlow(replay=0), a Channel buffers indefinitely until consumed.
    private val _incomingMessages = Channel<ProtoMessage>(Channel.UNLIMITED)
    val incomingMessages = _incomingMessages.receiveAsFlow()

    private val recentMessageIds = LinkedHashSet<String>(MAX_RECENT_IDS)

    fun connect(fingerprint: String) {
        if (socket?.connected() == true && currentFingerprint == fingerprint) return

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

            on("receive_message") { args ->
                try {
                    val raw = args[0]
                    val protoMessage: ProtoMessage? = when (raw) {
                        is ByteArray -> ProtoMessage.parseFrom(raw)
                        is JSONObject -> parseEnvelopeJson(raw)
                        is String -> parseEnvelopeJson(JSONObject(raw))
                        else -> null
                    }

                    if (protoMessage != null) {
                        val dedupKey = "${protoMessage.senderId}|${protoMessage.recipientId}|${protoMessage.timestamp}"

                        synchronized(recentMessageIds) {
                            if (recentMessageIds.contains(dedupKey)) return@on
                            if (recentMessageIds.size >= MAX_RECENT_IDS) {
                                val iterator = recentMessageIds.iterator()
                                if (iterator.hasNext()) {
                                    iterator.next()
                                    iterator.remove()
                                }
                            }
                            recentMessageIds.add(dedupKey)
                        }

                        _incomingMessages.trySend(protoMessage)
                    }
                } catch (_: Exception) {}
            }

            on("user_deleted") { args ->
                try {
                    val json = args[0] as JSONObject
                    val fp = json.getString("fingerprint")
                    _events.tryEmit(SocketEvent.UserDeleted(fp))
                } catch (_: Exception) {}
            }

            connect()
        }
    }

    private fun parseEnvelopeJson(json: JSONObject): ProtoMessage {
        val payloadB64 = json.getString("payload")
        val payloadBytes = android.util.Base64.decode(payloadB64, android.util.Base64.NO_WRAP)
        return ProtoMessage.parseFrom(payloadBytes)
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

    fun emitAck(messageUuid: String) {
        try {
            val json = JSONObject().apply {
                put("messageUuid", messageUuid)
            }
            socket?.emit("message_ack", json)
        } catch (_: Exception) {}
    }

    fun disconnect() {
        _connectionState.value = false
        currentFingerprint = null
        socket?.off()
        socket?.disconnect()
        socket = null
        synchronized(recentMessageIds) {
            recentMessageIds.clear()
        }
    }

    fun isConnected(): Boolean = socket?.connected() ?: false

    fun ensureConnected(fingerprint: String) {
        if (!isConnected()) {
            connect(fingerprint)
        }
    }

    companion object {
        private const val MAX_RECENT_IDS = 200
    }
}
