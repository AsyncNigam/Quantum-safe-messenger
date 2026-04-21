package com.nigdroid.quantummessenger.domain.usecase

import android.util.Log
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.data.local.ContactEntity
import com.nigdroid.quantummessenger.network.api.ContactApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case to synchronize contacts from the remote server to the local database.
 */
class SyncContactsUseCase @Inject constructor(
    private val contactApiService: ContactApiService,
    private val contactDao: ContactDao
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        try {
            // In a production app, we would retrieve the lastSyncTimestamp from local storage
            val lastSync = 0L 
            val response = contactApiService.syncContacts(lastSync)
            
            if (response.isSuccessful) {
                val remoteContacts = response.body()?.contacts ?: emptyList()
                val entities = remoteContacts.map { remote ->
                    ContactEntity(
                        userId = remote.userId,
                        phoneNumber = remote.phoneNumber,
                        displayName = remote.displayName,
                        mlKemPublicKey = remote.mlKemPublicKey,
                        mlDsaPublicKey = remote.mlDsaPublicKey,
                        x25519PublicKey = remote.x25519PublicKey,
                        ed25519PublicKey = remote.ed25519PublicKey,
                        lastSeen = System.currentTimeMillis()
                    )
                }
                if (entities.isNotEmpty()) {
                    contactDao.insertContacts(entities)
                }
                Log.d("SyncContactsUseCase", "Successfully synced ${entities.size} contacts")
            } else {
                Log.e("SyncContactsUseCase", "Sync failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("SyncContactsUseCase", "Exception during contact sync", e)
        }
    }
}
