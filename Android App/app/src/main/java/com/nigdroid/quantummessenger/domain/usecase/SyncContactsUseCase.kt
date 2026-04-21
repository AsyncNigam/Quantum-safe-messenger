package com.nigdroid.quantummessenger.domain.usecase

import android.util.Log
import com.nigdroid.quantummessenger.data.local.ContactDao
import com.nigdroid.quantummessenger.data.local.ContactEntity
import com.nigdroid.quantummessenger.network.api.ContactService
import com.nigdroid.quantummessenger.network.model.SyncRequest
import retrofit2.Response
import javax.inject.Inject

class SyncContactsUseCase @Inject constructor(
    private val contactService: ContactService,
    private val contactDao: ContactDao
) {
    suspend operator fun invoke() {
        try {
            // In a real app, we'd store and use the lastSyncTimestamp
            val response = contactService.syncContacts(SyncRequest(lastSyncTimestamp = 0))
            if (response.isSuccessful) {
                val remoteContacts = response.body()?.contacts ?: emptyList()
                val entities = remoteContacts.map {
                    ContactEntity(
                        userId = it.userId,
                        phoneNumber = it.phoneNumber,
                        displayName = it.displayName,
                        mlKemPublicKey = it.mlKemPublicKey,
                        mlDsaPublicKey = it.mlDsaPublicKey,
                        x25519PublicKey = it.x25519PublicKey,
                        ed25519PublicKey = it.ed25519PublicKey
                    )
                }
                contactDao.insertContacts(entities)
            }
        } catch (e: Exception) {
            Log.e("SyncContactsUseCase", "Failed to sync contacts", e)
        }
    }
}
