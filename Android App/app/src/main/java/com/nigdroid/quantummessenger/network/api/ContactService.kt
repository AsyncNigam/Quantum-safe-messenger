package com.nigdroid.quantummessenger.network.api

import com.nigdroid.quantummessenger.network.model.SyncRequest
import com.nigdroid.quantummessenger.network.model.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ContactService {
    @POST("keys/sync")
    suspend fun syncContacts(
        @Body request: SyncRequest
    ): Response<SyncResponse>
}
