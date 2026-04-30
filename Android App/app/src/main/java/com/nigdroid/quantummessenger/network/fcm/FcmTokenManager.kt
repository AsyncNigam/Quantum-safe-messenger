package com.nigdroid.quantummessenger.network.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.network.api.AuthenticationService
import com.nigdroid.quantummessenger.network.api.FcmTokenRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    private val sessionManager: SessionManager,
    private val authService: AuthenticationService
) {

    suspend fun syncToken() {
        val token = FirebaseMessaging.getInstance().token.await()
        sessionManager.setFcmToken(token)

        val fingerprint = sessionManager.textFingerprint.firstOrNull() ?: return
        authService.registerFcmToken(
            "Bearer $fingerprint",
            FcmTokenRequest(fcmToken = token)
        )
    }
}
