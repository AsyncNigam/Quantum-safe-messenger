package com.nigdroid.quantummessenger.network.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nigdroid.quantummessenger.MainActivity
import com.nigdroid.quantummessenger.R
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.network.api.AuthenticationService
import com.nigdroid.quantummessenger.network.api.FcmTokenRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class QuantumFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var authService: AuthenticationService
    @Inject lateinit var webSocketManager: WebSocketManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "quantum_messages"
        const val CHANNEL_NAME = "Quantum Messages"
        const val CHANNEL_DESC = "Encrypted message notifications"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    setShowBadge(true)
                }
                val mgr = context.getSystemService(NotificationManager::class.java)
                mgr.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                sessionManager.setFcmToken(token)

                val fingerprint = sessionManager.textFingerprint.firstOrNull()
                if (fingerprint != null) {
                    authService.registerFcmToken(
                        "Bearer $fingerprint",
                        FcmTokenRequest(fcmToken = token)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"] ?: "new_message"
        val senderFingerprint = data["senderFingerprint"] ?: "unknown"

        // Trigger background WebSocket connect to pull queued messages from Redis.
        // This ensures messages arrive even when the app was killed.
        triggerBackgroundSync()

        when (type) {
            "new_message" -> showMessageNotification(senderFingerprint)
            "contact_request" -> showContactRequestNotification(senderFingerprint)
        }
    }

    private fun triggerBackgroundSync() {
        serviceScope.launch {
            try {
                val fingerprint = sessionManager.textFingerprint.firstOrNull() ?: return@launch
                if (!webSocketManager.isConnected()) {
                    webSocketManager.connect(fingerprint)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showMessageNotification(senderFingerprint: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "chat")
            putExtra("participant_id", senderFingerprint)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, senderFingerprint.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortFp = senderFingerprint.take(8).uppercase()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.qlogo)
            .setContentTitle("\uD83D\uDD10 New Encrypted Message")
            .setContentText("From: $shortFp…")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(senderFingerprint.hashCode(), notification)
    }

    private fun showContactRequestNotification(senderFingerprint: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, senderFingerprint.hashCode() + 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortFp = senderFingerprint.take(8).uppercase()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.qlogo)
            .setContentTitle("\uD83E\uDD1D New Contact Request")
            .setContentText("From: $shortFp…")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(senderFingerprint.hashCode() + 1, notification)
    }
}
