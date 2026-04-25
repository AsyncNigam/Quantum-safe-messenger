package com.nigdroid.quantummessenger.network.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.network.api.AuthenticationService
import com.nigdroid.quantummessenger.network.api.FcmTokenRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper to fetch and sync the FCM token with the backend.
 * Call [syncToken] after registration or on app startup.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val sessionManager: SessionManager,
    private val authService: AuthenticationService
) {
    companion object {
        private const val TAG = "FcmTokenManager"
    }

    /**
     * Fetches the current FCM token from Firebase and syncs it with the backend.
     * Should be called:
     *  1. After successful registration
     *  2. On app startup (in case the token was refreshed while the app was dead)
     */
    suspend fun syncToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token obtained (${token.take(10)}…)")

            // Store locally
            sessionManager.setFcmToken(token)

            // Sync with backend
            val fingerprint = sessionManager.textFingerprint.firstOrNull() ?: return
            val response = authService.registerFcmToken(
                "Bearer $fingerprint",
                FcmTokenRequest(fcmToken = token)
            )
            if (response.isSuccessful) {
                Log.d(TAG, "✅ FCM token synced with backend")
            } else {
                Log.w(TAG, "⚠️ Backend FCM sync failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync FCM token: ${e.message}")
        }
    }
}
