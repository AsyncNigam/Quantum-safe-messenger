package com.nigdroid.quantummessenger.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthenticationService {


    @GET("health")
    suspend fun checkHealth(): Response<GenericResponse>


    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>


    @GET("api/keys/sync")
    suspend fun syncKeys(
        @Query("page")  page: Int,
        @Query("limit") limit: Int
    ): Response<KeySyncResponse>


    @POST("api/keys/upload")
    suspend fun uploadKeyBundle(
        @Header("Authorization") fingerprint: String,
        @Body request: KeyUploadRequest
    ): Response<GenericResponse>


    @GET("api/auth/lookup/{fingerprint}")
    suspend fun lookupUser(
        @Header("Authorization") auth: String,
        @retrofit2.http.Path("fingerprint") fingerprint: String
    ): Response<LookupResponse>


    @POST("api/auth/fcm-token")
    suspend fun registerFcmToken(
        @Header("Authorization") auth: String,
        @Body request: FcmTokenRequest
    ): Response<GenericResponse>


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

