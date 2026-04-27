package com.nigdroid.quantummessenger.data.repository

import android.util.Base64
import com.nigdroid.quantummessenger.domain.model.AuthenticationResult
import com.nigdroid.quantummessenger.domain.model.Identity
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult
import com.nigdroid.quantummessenger.domain.repository.AuthRepository
import com.nigdroid.quantummessenger.domain.usecase.GenerateIdentityUseCase
import com.nigdroid.quantummessenger.network.api.AuthenticationService
import com.nigdroid.quantummessenger.network.api.RegisterRequest
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nigdroid.quantummessenger.network.fcm.FcmTokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthenticationService,
    private val generateIdentityUseCase: GenerateIdentityUseCase,
    private val sessionManager: SessionManager,
    private val fcmTokenManager: FcmTokenManager
) : AuthRepository {

    /**
     * Full ZK registration flow:
     *  1. Generate ML-KEM + X25519 keypairs (on device, in Keystore)
     *  2. POST the Base64 public keys to /auth/register
     *  3. Receive the textFingerprint from the server
     *  4. Persist the fingerprint in encrypted DataStore
     *  5. Mark the user as registered
     */
    override suspend fun generateAndRegisterIdentity(): IdentityGenerationResult =
        withContext(Dispatchers.IO) {
            // Step 1: Generate keys
            val keysResult = generateIdentityUseCase()
            if (keysResult.isFailure) {
                return@withContext IdentityGenerationResult.Error(
                    exception = keysResult.exceptionOrNull() as? Exception
                        ?: Exception("Key generation failed"),
                    message   = keysResult.exceptionOrNull()?.message ?: "Key generation failed"
                )
            }

            val keys = keysResult.getOrThrow()

            // Step 2: Encode and register with backend
            val mlKemB64   = Base64.encodeToString(keys.mlKemPublicKey,  Base64.NO_WRAP)
            val x25519B64  = Base64.encodeToString(keys.x25519PublicKey, Base64.NO_WRAP)

            try {
                val response = authService.register(
                    RegisterRequest(
                        mlKemPublicKey  = mlKemB64,
                        x25519PublicKey = x25519B64
                    )
                )

                if (!response.isSuccessful || response.body() == null) {
                    return@withContext IdentityGenerationResult.Error(
                        exception = Exception("Registration failed: HTTP ${response.code()}"),
                        message   = "Backend registration failed (${response.code()})"
                    )
                }

                val body = response.body()!!
                val fingerprint = body.textFingerprint

                if (fingerprint == null) {
                    return@withContext IdentityGenerationResult.Error(
                        exception = Exception("Server response missing fingerprint"),
                        message   = "Server response missing fingerprint"
                    )
                }

                // Step 3: Build the local Identity object
                val identity = Identity(
                    textFingerprint  = fingerprint,
                    mlKemPublicKey   = keys.mlKemPublicKey,
                    x25519PublicKey  = keys.x25519PublicKey
                )

                // Step 4: Persist fingerprint and public keys in encrypted DataStore
                sessionManager.setTextFingerprint(fingerprint)
                sessionManager.setPublicKeys(mlKemB64, x25519B64)
                sessionManager.setUserRegistered(true)

                // Step 5: Sync FCM token with backend for push notifications
                try { fcmTokenManager.syncToken() } catch (_: Exception) { }

                IdentityGenerationResult.Success(identity)

            } catch (e: Exception) {
                IdentityGenerationResult.Error(
                    exception = e,
                    message   = "Network error: ${e.localizedMessage}"
                )
            }
        }

    override suspend fun getStoredIdentity(): Identity? {
        // In a full implementation, reconstruct Identity from DataStore + Keystore
        // For now return null — the fingerprint flow drives navigation
        return null
    }

    override suspend fun isRegistered(): Boolean =
        withContext(Dispatchers.IO) {
            // Queried reactively via SessionManager.isUserRegistered flow in MainViewModel
            false
        }

    override suspend fun clearIdentity() {
        withContext(Dispatchers.IO) {
            sessionManager.setTextFingerprint(null)
            sessionManager.setUserRegistered(false)
        }
    }
}
