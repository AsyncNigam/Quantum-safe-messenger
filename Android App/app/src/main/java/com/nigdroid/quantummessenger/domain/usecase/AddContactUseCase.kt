package com.nigdroid.quantummessenger.domain.usecase

import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.data.local.ContactEntity
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import com.nigdroid.quantummessenger.network.api.AuthenticationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * AddContactUseCase — fetches a user's public keys from the server
 * by their Text Fingerprint and saves them locally as a ContactEntity.
 */
class AddContactUseCase @Inject constructor(
    private val authService: AuthenticationService,
    private val contactDao: ContactDao,
    private val sessionManager: SessionManager
) {
    sealed class Result {
        data class Success(val fingerprint: String) : Result()
        data class AlreadyExists(val fingerprint: String) : Result()
        data class Error(val message: String) : Result()
        object SelfAdd : Result()
    }

    suspend operator fun invoke(targetFingerprint: String): Result = withContext(Dispatchers.IO) {
        try {
            val cleaned = targetFingerprint.trim().lowercase()

            // Validate format
            if (cleaned.length != 64 || !cleaned.matches(Regex("^[a-f0-9]+$"))) {
                return@withContext Result.Error("Invalid fingerprint format — must be 64 hex characters.")
            }

            // Prevent adding yourself
            val ownFingerprint = sessionManager.textFingerprint.firstOrNull()
            if (cleaned == ownFingerprint) {
                return@withContext Result.SelfAdd
            }

            // Check if already saved locally
            val existing = contactDao.getContactById(cleaned)
            if (existing != null) {
                return@withContext Result.AlreadyExists(cleaned)
            }

            // Fetch from server
            val bearerToken = "Bearer $ownFingerprint"
            val response = authService.lookupUser(bearerToken, cleaned)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                return@withContext when (response.code()) {
                    404  -> Result.Error("User not found — they may not have registered yet.")
                    401  -> Result.Error("Authentication failed — please restart the app.")
                    else -> Result.Error("Server error: $errorBody")
                }
            }

            val body = response.body() ?: return@withContext Result.Error("Empty response from server.")

            // Save to local Room database
            val contact = ContactEntity(
                userId         = body.fingerprint,
                mlKemPublicKey = body.mlKemPublicKey,
                x25519PublicKey = body.x25519PublicKey
            )
            contactDao.insertContact(contact)

            Result.Success(body.fingerprint)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to add contact.")
        }
    }
}
