package com.nigdroid.quantummessenger.domain.repository

import com.nigdroid.quantummessenger.domain.model.AuthenticationResult
import com.nigdroid.quantummessenger.domain.model.Identity
import com.nigdroid.quantummessenger.domain.model.IdentityGenerationResult

/**
 * Repository interface for Zero-Knowledge anonymous identity operations.
 *
 * No Google, no OAuth, no email — purely cryptographic.
 */
interface AuthRepository {

    /**
     * Generate ML-KEM and X25519 keys, register with the backend,
     * and persist the returned textFingerprint locally.
     *
     * @return IdentityGenerationResult.Success with the complete Identity, or Error
     */
    suspend fun generateAndRegisterIdentity(): IdentityGenerationResult

    /**
     * Retrieve the locally stored identity (after first registration).
     */
    suspend fun getStoredIdentity(): Identity?

    /**
     * Whether the user has already registered on this device.
     */
    suspend fun isRegistered(): Boolean

    /**
     * Wipe all local identity data (factory reset).
     */
    suspend fun clearIdentity()

    /**
     * Delete the user's account on the backend (soft-delete),
     * then wipe all local identity data.
     *
     * @return true if backend deletion succeeded, false on error
     */
    suspend fun deleteAccount(): Boolean
}
