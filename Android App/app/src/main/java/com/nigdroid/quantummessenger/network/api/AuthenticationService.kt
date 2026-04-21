package com.nigdroid.quantummessenger.network.api

import com.nigdroid.quantummessenger.domain.model.AuthRegisterRequest
import com.nigdroid.quantummessenger.domain.model.AuthRegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API Service for Authentication Endpoints
 *
 * Communicates with the secure authentication server.
 * All requests are made over TLS 1.3+ with certificate pinning.
 */
interface AuthenticationService {

    /**
     * Register new user with generated cryptographic identity
     *
     * Endpoint: POST /auth/register
     *
     * Request Body:
     * {
     *   "phoneNumber": "+1234567890",
     *   "mlKemPublicKey": "base64_encoded_key",
     *   "mlDsaPublicKey": "base64_encoded_key",
     *   "x25519PublicKey": "base64_encoded_key",
     *   "ed25519PublicKey": "base64_encoded_key",
     *   "deviceToken": "fcm_token_optional",
     *   "deviceName": "Pixel 8 Pro"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "userId": "uuid-string",
     *   "message": "Registration successful",
     *   "serverPublicKey": "base64_encoded_x25519_public_key",
     *   "challengeNonce": "base64_encoded_nonce_for_proof_of_identity"
     * }
     *
     * @param request Registration request with user data and public keys
     * @return Response containing userId and server's public key
     *
     * Possible HTTP Status Codes:
     * - 200: Registration successful
     * - 400: Invalid input (malformed request)
     * - 409: PhoneNumber already registered
     * - 422: Invalid key format or size
     * - 500: Server error
     */
    @POST("auth/register")
    suspend fun registerUser(
        @Body request: AuthRegisterRequest
    ): Response<AuthRegisterResponse>

    /**
     * Verify ownership of phone number (OTP challenge)
     *
     * Endpoint: POST /auth/verify-phone
     *
     * Request Body:
     * {
     *   "phoneNumber": "+1234567890",
     *   "otpCode": "123456",
     *   "signature": "base64_encoded_ed25519_signature"
     * }
     *
     * @param phoneNumber User's phone number
     * @param otpCode One-time password sent to phone
     * @param signature Ed25519 signature to prove key ownership
     * @return Response confirming phone verification
     */
    @POST("auth/verify-phone")
    suspend fun verifyPhoneNumber(
        phoneNumber: String,
        otpCode: String,
        signature: String
    ): Response<Map<String, Any>>

    /**
     * Authenticate user with signature proof
     *
     * Endpoint: POST /auth/authenticate
     *
     * Uses challenge-response authentication:
     * 1. Client requests auth challenge
     * 2. Server returns random nonce
     * 3. Client signs nonce with Ed25519 private key
     * 4. Server verifies signature using client's public key
     *
     * @param userId User's ID from registration
     * @param signature Ed25519 signature of challenge nonce
     * @return Response containing JWT session token
     */
    @POST("auth/authenticate")
    suspend fun authenticateUser(
        userId: String,
        signature: String
    ): Response<Map<String, String>>
}

