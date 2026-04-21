package com.nigdroid.quantummessenger.network.api

import com.nigdroid.quantummessenger.network.model.SyncResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for contact-related operations.
 */
interface ContactApiService {
    /**
     * Fetches public keys and contact updates from the server.
     */
    @GET("keys/sync")
    suspend fun syncContacts(
        @Query("lastSyncTimestamp") lastSyncTimestamp: Long
    ): Response<SyncResponse>
}
