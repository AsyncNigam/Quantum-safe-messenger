package com.nigdroid.quantummessenger.data.repository

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nigdroid.quantummessenger.domain.model.AuthRegisterRequest
import com.nigdroid.quantummessenger.domain.model.AuthenticationResult
import com.nigdroid.quantummessenger.domain.model.Identity
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult
import com.nigdroid.quantummessenger.domain.repository.AuthRepository
import com.nigdroid.quantummessenger.domain.usecase.GenerateIdentityUseCase
import com.nigdroid.quantummessenger.network.api.AuthenticationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.nigdroid.quantummessenger.network.api.KeyUploadRequest
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo

/**
 * Implementation of AuthRepository
 *
 * Orchestrates:
 * 1. Supabase Email/Password Authentication
 * 2. Cryptographic identity generation
 * 3. Server registration (Key bundle upload)
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: AuthenticationService,
    private val generateIdentityUseCase: GenerateIdentityUseCase,
    private val sessionManager: SessionManager,
    private val supabaseAuth: Auth
) : AuthRepository {

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "quantum_messenger_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun generateIdentity(identifier: String): IdentityGenerationResult =
        withContext(Dispatchers.Default) {
            generateIdentityUseCase.invoke(identifier)
        }

    override suspend fun registerIdentity(identity: Identity): AuthenticationResult =
        withContext(Dispatchers.IO) {
            try {
                // 1. Get the current Supabase session
                val session = when (val status = supabaseAuth.sessionStatus.value) {
                    is SessionStatus.Authenticated -> status.session
                    else -> null
                }

                if (session == null) {
                    return@withContext AuthenticationResult.Error(
                        exception = Exception("No active session"),
                        message = "Please sign in or confirm your email to continue."
                    )
                }

                val jwtToken = "Bearer ${session.accessToken}"
                // Store token locally
                sessionManager.setAuthToken(session.accessToken)

                // 2. Prepare key upload request
                val request = KeyUploadRequest(
                    x25519PublicKey = Base64.encodeToString(identity.x25519PublicKey, Base64.NO_WRAP),
                    mlKemPublicKey = Base64.encodeToString(identity.mlKemPublicKey, Base64.NO_WRAP),
                    ed25519Signature = Base64.encodeToString(identity.ed25519PublicKey, Base64.NO_WRAP),
                    mlDsaSignature = Base64.encodeToString(identity.mlDsaPublicKey, Base64.NO_WRAP)
                )

                // 3. Upload to backend
                val response = authService.uploadKeyBundle(jwtToken, request)

                if (response.isSuccessful) {
                    storeIdentity(identity)
                    sessionManager.setUserRegistered(true)

                    AuthenticationResult.Success(
                        userId = identity.userId,
                        identity = identity
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    AuthenticationResult.Error(
                        exception = Exception("Server error: ${response.code()}"),
                        message = "Backend upload failed (${response.code()}). Check if your backend is running."
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                AuthenticationResult.Error(e, "Connection failed: ${e.localizedMessage}")
            }
        }

    override suspend fun signInAnonymously(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabaseAuth.signInAnonymously()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabaseAuth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                supabaseAuth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun authenticateUser(userId: String): AuthenticationResult =
        withContext(Dispatchers.IO) {
            try {
                AuthenticationResult.Success(userId, Identity(
                    userId = userId,
                    identifier = "",
                    mlKemPublicKey = ByteArray(0),
                    mlDsaPublicKey = ByteArray(0),
                    x25519PublicKey = ByteArray(0),
                    ed25519PublicKey = ByteArray(0)
                ))
            } catch (e: Exception) {
                AuthenticationResult.Error(e, "Authentication failed")
            }
        }

    override suspend fun isUserRegistered(identifier: String): Boolean =
        withContext(Dispatchers.IO) {
            sharedPreferences.contains("identity_$identifier")
        }

    override suspend fun getStoredIdentity(userId: String): Identity? = null

    override suspend fun clearAuthentication() {
        withContext(Dispatchers.IO) {
            supabaseAuth.signOut()
            sharedPreferences.edit().clear().apply()
            sessionManager.setAuthToken(null)
            sessionManager.setUserRegistered(false)
        }
    }

    private fun storeIdentity(identity: Identity) {
        sharedPreferences.edit().apply {
            putString("user_id", identity.userId)
            putString("identifier", identity.identifier)
            putString("created_at", identity.createdAt.toString())
            apply()
        }
    }
}
