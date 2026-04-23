package com.nigdroid.quantummessenger.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthenticationService {
    @GET("health")
    suspend fun checkHealth(): Response<GenericResponse>

    @GET("keys/sync")
    suspend fun syncKeys(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<KeySyncResponse>

    @POST("keys/upload")
    suspend fun uploadKeyBundle(
        @Header("Authorization") token: String,
        @Body request: KeyUploadRequest
    ): Response<GenericResponse>
}

data class GenericResponse(
    val status: String? = null,
    val success: Boolean? = null,
    val message: String? = null
)

data class KeySyncResponse(
    val page: Int,
    val total: Int,
    val data: List<KeyBundleDto>
)

data class KeyBundleDto(
    val userId: String,
    val x25519PublicKey: String,
    val mlKemPublicKey: String,
    val createdAt: String
)

data class KeyUploadRequest(
    val x25519PublicKey: String,
    val mlKemPublicKey: String,
    val ed25519Signature: String,
    val mlDsaSignature: String
)
