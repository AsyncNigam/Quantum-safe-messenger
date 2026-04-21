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
     * @param phoneNumber User's phone number in E.164 format
     * @return IdentityGenerationResult with generated identity or error
     */
    suspend fun generateIdentity(phoneNumber: String): IdentityGenerationResult

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
     * @param phoneNumber Phone number to check
     * @return true if registered, false otherwise
     */
    suspend fun isUserRegistered(phoneNumber: String): Boolean

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
}

