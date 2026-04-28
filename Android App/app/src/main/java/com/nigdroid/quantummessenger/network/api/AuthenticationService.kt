package com.nigdroid.quantummessenger.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthenticationService {

    /** GET /health — liveness check */
    @GET("health")
    suspend fun checkHealth(): Response<GenericResponse>

    /**
     * POST /auth/register
     *
     * Zero-Knowledge identity registration.
     * Sends ML-KEM and X25519 public keys, receives a textFingerprint.
     */
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    /** GET /keys/sync — fetch paginated public key bundles */
    @GET("api/keys/sync")
    suspend fun syncKeys(
        @Query("page")  page: Int,
        @Query("limit") limit: Int
    ): Response<KeySyncResponse>

    /** POST /api/keys/upload — upload full hybrid key bundle (after registration) */
    @POST("api/keys/upload")
    suspend fun uploadKeyBundle(
        @Header("Authorization") fingerprint: String,
        @Body request: KeyUploadRequest
    ): Response<GenericResponse>

    /**
     * GET /auth/lookup/{fingerprint}
     *
     * Contact discovery — fetch public keys for a given text fingerprint.
     * Requires Bearer auth.
     */
    @GET("api/auth/lookup/{fingerprint}")
    suspend fun lookupUser(
        @Header("Authorization") auth: String,
        @retrofit2.http.Path("fingerprint") fingerprint: String
    ): Response<LookupResponse>

    /**
     * POST /api/auth/fcm-token
     *
     * Register or update FCM token for push notifications.
     * Requires Bearer auth.
     */
    @POST("api/auth/fcm-token")
    suspend fun registerFcmToken(
        @Header("Authorization") auth: String,
        @Body request: FcmTokenRequest
    ): Response<GenericResponse>

    /**
     * DELETE /api/auth/account
     *
     * Soft-delete the authenticated user's account.
     * Wipes public keys from backend but keeps the fingerprint row
     * so contacts can detect "Deleted Account".
     * Requires Bearer auth.
     */
    @DELETE("api/auth/account")
    suspend fun deleteAccount(
        @Header("Authorization") auth: String
    ): Response<GenericResponse>
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

data class GenericResponse(
    val status: String? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class RegisterRequest(
    val mlKemPublicKey: String,   // Base64
    val x25519PublicKey: String   // Base64
)

data class RegisterResponse(
    val success: Boolean,
    val textFingerprint: String?
)

data class KeySyncResponse(
    val page: Int,
    val total: Int,
    val data: List<KeyBundleDto>
)

data class KeyBundleDto(
    val fingerprint: String?,
    val x25519PublicKey: String?,
    val mlKemPublicKey: String?,
    val createdAt: String?
)

data class KeyUploadRequest(
    val x25519PublicKey: String,
    val mlKemPublicKey: String,
    val ed25519Signature: String,
    val mlDsaSignature: String
)

data class LookupResponse(
    val success: Boolean,
    val fingerprint: String?,
    val mlKemPublicKey: String?,
    val x25519PublicKey: String?,
    val deleted: Boolean? = false,
    val deletedAt: String? = null
)

data class FcmTokenRequest(
    val fcmToken: String
)

