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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository
 *
 * Orchestrates:
 * 1. Cryptographic identity generation
 * 2. Server registration
 * 3. User authentication
 * 4. Secure local storage of identities
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val context: Context,
    private val authService: AuthenticationService,
    private val generateIdentityUseCase: GenerateIdentityUseCase
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

    override suspend fun generateIdentity(phoneNumber: String): IdentityGenerationResult =
        withContext(Dispatchers.Default) {
            generateIdentityUseCase.invoke(phoneNumber)
        }

    override suspend fun registerIdentity(identity: Identity): AuthenticationResult =
        withContext(Dispatchers.IO) {
            try {
                // Create registration request with Base64-encoded public keys
                val request = AuthRegisterRequest(
                    phoneNumber = identity.phoneNumber,
                    mlKemPublicKey = Base64.encodeToString(identity.mlKemPublicKey, Base64.NO_WRAP),
                    mlDsaPublicKey = Base64.encodeToString(identity.mlDsaPublicKey, Base64.NO_WRAP),
                    x25519PublicKey = Base64.encodeToString(identity.x25519PublicKey, Base64.NO_WRAP),
                    ed25519PublicKey = Base64.encodeToString(identity.ed25519PublicKey, Base64.NO_WRAP),
                    deviceName = android.os.Build.MODEL
                )

                // Send registration request
                val response = authService.registerUser(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val responseBody = response.body()!!

                    // Store identity locally
                    storeIdentity(identity)

                    // Store server's public key for future communication
                    responseBody.serverPublicKey?.let {
                        storeServerPublicKey(it)
                    }

                    AuthenticationResult.Success(
                        userId = responseBody.userId,
                        identity = identity
                    )
                } else {
                    AuthenticationResult.Error(
                        exception = Exception("Registration failed: ${response.code()}"),
                        message = response.body()?.message ?: "Unknown error occurred"
                    )
                }

            } catch (e: Exception) {
                AuthenticationResult.NetworkError
            }
        }

    override suspend fun authenticateUser(userId: String): AuthenticationResult =
        withContext(Dispatchers.IO) {
            try {
                // In production, implement challenge-response authentication
                // For now, returning success placeholder
                AuthenticationResult.Success(userId, Identity(
                    userId = userId,
                    phoneNumber = "",
                    mlKemPublicKey = ByteArray(0),
                    mlDsaPublicKey = ByteArray(0),
                    x25519PublicKey = ByteArray(0),
                    ed25519PublicKey = ByteArray(0)
                ))

            } catch (e: Exception) {
                AuthenticationResult.Error(e, "Authentication failed")
            }
        }

    override suspend fun isUserRegistered(phoneNumber: String): Boolean =
        withContext(Dispatchers.IO) {
            sharedPreferences.contains("identity_$phoneNumber")
        }

    override suspend fun getStoredIdentity(userId: String): Identity? =
        withContext(Dispatchers.IO) {
            val json = sharedPreferences.getString("identity_$userId", null) ?: return@withContext null
            // In production, deserialize using kotlinx.serialization
            null
        }

    override suspend fun clearAuthentication() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().clear().apply()
        }
    }

    /**
     * Store identity locally in encrypted shared preferences
     */
    private fun storeIdentity(identity: Identity) {
        sharedPreferences.edit().apply {
            // Store identity data (in production, use proper serialization)
            putString("user_id", identity.userId)
            putString("phone_number", identity.phoneNumber)
            putString("created_at", identity.createdAt.toString())
            apply()
        }
    }

    /**
     * Store server's public key for future communication
     */
    private fun storeServerPublicKey(publicKeyBase64: String) {
        sharedPreferences.edit().apply {
            putString("server_public_key", publicKeyBase64)
            apply()
        }
    }
}

