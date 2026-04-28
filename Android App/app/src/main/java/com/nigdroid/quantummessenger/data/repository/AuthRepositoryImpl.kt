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
import com.nigdroid.quantummessenger.data.security.VaultWipeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import com.nigdroid.quantummessenger.network.fcm.FcmTokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthenticationService,
    private val generateIdentityUseCase: GenerateIdentityUseCase,
    private val sessionManager: SessionManager,
    private val fcmTokenManager: FcmTokenManager,
    private val vaultWipeManager: VaultWipeManager
) : AuthRepository {

    /**
     * Full ZK registration flow:
     *  1. Check if keys already exist in DataStore (logout scenario)
     *  2. If not, generate ML-KEM + X25519 keypairs (on device, in Keystore)
     *  3. POST the Base64 public keys to /auth/register (idempotent)
     *  4. Receive the textFingerprint from the server
     *  5. Persist the fingerprint in encrypted DataStore
     *  6. Mark the user as registered
     */
    override suspend fun generateAndRegisterIdentity(): IdentityGenerationResult =
        withContext(Dispatchers.IO) {
            // Step 1: Check if we already have keys (user logged out, not deleted)
            val existingMlKem   = sessionManager.mlKemPublicKey.firstOrNull()
            val existingX25519  = sessionManager.x25519PublicKey.firstOrNull()

            val mlKemB64: String
            val x25519B64: String
            val mlKemBytes: ByteArray
            val x25519Bytes: ByteArray

            if (existingMlKem != null && existingX25519 != null) {
                // Re-login: reuse existing keys
                mlKemB64    = existingMlKem
                x25519B64   = existingX25519
                mlKemBytes  = Base64.decode(existingMlKem, Base64.NO_WRAP)
                x25519Bytes = Base64.decode(existingX25519, Base64.NO_WRAP)
            } else {
                // Fresh registration: generate new keys
                val keysResult = generateIdentityUseCase()
                if (keysResult.isFailure) {
                    return@withContext IdentityGenerationResult.Error(
                        exception = keysResult.exceptionOrNull() as? Exception
                            ?: Exception("Key generation failed"),
                        message   = keysResult.exceptionOrNull()?.message ?: "Key generation failed"
                    )
                }
                val keys = keysResult.getOrThrow()
                mlKemBytes  = keys.mlKemPublicKey
                x25519Bytes = keys.x25519PublicKey
                mlKemB64    = Base64.encodeToString(mlKemBytes, Base64.NO_WRAP)
                x25519B64   = Base64.encodeToString(x25519Bytes, Base64.NO_WRAP)
            }

            // Step 2: Register with backend (idempotent upsert — safe to call again)
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
                    mlKemPublicKey   = mlKemBytes,
                    x25519PublicKey  = x25519Bytes
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

    /**
     * Delete the account on the backend (soft-delete), then wipe all local data.
     *
     * After wipe, the app process is killed to ensure stale Hilt singletons
     * (especially the Room database) are re-created on next launch.
     *
     * @return true if the backend confirmed deletion, false on network/server error
     */
    override suspend fun deleteAccount(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the current fingerprint for the Bearer token
            val fingerprint = sessionManager.textFingerprint.firstOrNull()
                ?: return@withContext false

            val response = authService.deleteAccount("Bearer $fingerprint")

            if (!response.isSuccessful) {
                android.util.Log.e("AuthRepo", "Delete account failed: HTTP ${response.code()}")
                return@withContext false
            }

            android.util.Log.w("AuthRepo", "✅ Backend confirmed deletion — wiping local data")

            // Backend confirmed deletion — now wipe all local data
            vaultWipeManager.executeZeroTrustWipe()

            true
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "Delete account error: ${e.message}")
            false
        }
    }
}

