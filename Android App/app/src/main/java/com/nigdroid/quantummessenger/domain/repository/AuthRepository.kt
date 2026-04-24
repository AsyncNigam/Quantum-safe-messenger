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
     * @param userId The unique user ID from the auth provider (Supabase)
     * @return IdentityGenerationResult with generated identity or error
     */
    suspend fun generateIdentity(identifier: String, userId: String): IdentityGenerationResult

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
     * Get the current authenticated user's ID
     */
    suspend fun getCurrentUserId(): String?

    /**
     * Sign in anonymously to Supabase to get a valid JWT
     */
    suspend fun signInAnonymously(): Result<Unit>

    /**
     * Sign in with Google ID Token via Supabase
     *
     * @param idToken Google ID Token from Credential Manager
     * @param nonce The nonce used to generate the ID token
     * @return Result of the operation
     */
    suspend fun signInWithGoogle(idToken: String, nonce: String? = null): Result<Unit>
}
