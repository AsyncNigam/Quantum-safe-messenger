package com.nigdroid.quantummessenger.network.api

import com.nigdroid.quantummessenger.domain.model.AuthRegisterRequest
import com.nigdroid.quantummessenger.domain.model.AuthRegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthenticationService {
    @POST("auth/register")
    suspend fun registerUser(
        @Body request: AuthRegisterRequest
    ): Response<AuthRegisterResponse>

    @POST("auth/verify-phone")
    suspend fun verifyPhoneNumber(
        @retrofit2.http.Query("phoneNumber") phoneNumber: String,
        @retrofit2.http.Query("otpCode") otpCode: String,
        @retrofit2.http.Query("signature") signature: String
    ): Response<GenericResponse>

    @POST("auth/authenticate")
    suspend fun authenticateUser(
        @retrofit2.http.Query("userId") userId: String,
        @retrofit2.http.Query("signature") signature: String
    ): Response<AuthResponse>
}

data class GenericResponse(
    val status: String,
    val message: String
)

data class AuthResponse(
    val token: String,
    val userId: String
)
