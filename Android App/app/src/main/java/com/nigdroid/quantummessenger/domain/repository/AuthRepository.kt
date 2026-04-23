package com.nigdroid.quantummessenger.domain.repository

import com.nigdroid.quantummessenger.domain.model.AuthenticationResult
import com.nigdroid.quantummessenger.domain.model.Identity
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult

/**
 * Repository interface for authentication operations
 *
 * Defines the contract for identity generation, registration, and authentication
 */
interface AuthRepository {

    /**
     * Generate cryptographic identity for new user
     *
     * @param identifier User's identifier (Email or Phone)
     * @return IdentityGenerationResult with generated identity or error
     */
    suspend fun generateIdentity(identifier: String): IdentityGenerationResult

    /**
     * Register generated identity with authentication server
     *
     * @param identity Generated identity with public keys
     * @return AuthenticationResult with registration outcome
     */
    suspend fun registerIdentity(identity: Identity): AuthenticationResult

    /**
     * Authenticate user with challenge-response protocol
     *
     * @param userId User's ID from previous registration
     * @return AuthenticationResult with authentication outcome
     */
    suspend fun authenticateUser(userId: String): AuthenticationResult

    /**
     * Check if user is already registered
     *
     * @param identifier Identifier to check
     * @return true if registered, false otherwise
     */
    suspend fun isUserRegistered(identifier: String): Boolean

    /**
     * Retrieve stored identity (after registration)
     *
     * @param userId User's ID
     * @return Identity if found, null otherwise
     */
    suspend fun getStoredIdentity(userId: String): Identity?

    /**
     * Clear all authentication data (logout)
     */
    suspend fun clearAuthentication()

    /**
     * Sign in anonymously to Supabase to get a valid JWT
     */
    suspend fun signInAnonymously(): Result<Unit>

    /**
     * Start phone number authentication by sending an OTP
     *
     * @param phoneNumber Phone number in E.164 format
     * @return Result of the operation
     */
    // suspend fun sendOtpToPhone(phoneNumber: String): Result<Unit>

    /**
     * Verify the OTP sent to the phone number
     *
     * @param phoneNumber Phone number in E.164 format
     * @param otp The one-time password received
     * @return Result of the operation
     */
    // suspend fun verifyOtp(phoneNumber: String, otp: String): Result<Unit>

    /**
     * Sign in with email and password via Supabase
     *
     * @param email User's email address
     * @param password User's password
     * @return Result of the operation
     */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /**
     * Sign up with email and password via Supabase
     *
     * @param email User's email address
     * @param password User's password
     * @return Result of the operation
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>
}

