package com.nigdroid.quantummessenger.domain.usecase

import android.util.Log
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.data.local.ContactEntity
import com.nigdroid.quantummessenger.network.api.AuthenticationService
import com.nigdroid.quantummessenger.data.local.prefs.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case to synchronize contacts from the remote server to the local database.
 * Updated for ZK architecture — uses key sync endpoint instead of phone-based contacts.
 */
class SyncContactsUseCase @Inject constructor(
    private val authService: AuthenticationService,
    private val contactDao: ContactDao,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        try {
            val ownFingerprint = sessionManager.textFingerprint.firstOrNull()
            if (ownFingerprint == null) {
                Log.w("SyncContacts", "No fingerprint — user not registered")
                return@withContext
            }

            val bearerToken = "Bearer $ownFingerprint"
            val response = authService.syncKeys(page = 1, limit = 100)

            if (response.isSuccessful) {
                val bundles = response.body()?.data ?: emptyList()
                val entities = bundles
                    .filter { it.fingerprint != null && it.fingerprint != ownFingerprint }
                    .mapNotNull { bundle ->
                        val fp = bundle.fingerprint
                        val ml = bundle.mlKemPublicKey
                        val x2 = bundle.x25519PublicKey
                        
                        if (fp != null && ml != null && x2 != null) {
                            ContactEntity(
                                userId          = fp,
                                mlKemPublicKey  = ml,
                                x25519PublicKey = x2
                            )
                        } else {
                            null
                        }
                    }
                if (entities.isNotEmpty()) {
                    contactDao.insertContacts(entities)
                }
                Log.d("SyncContacts", "Synced ${entities.size} contacts")
            } else {
                Log.e("SyncContacts", "Sync failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SyncContacts", "Exception during sync", e)
        }
    }
}
