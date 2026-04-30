package com.nigdroid.quantummessenger.network.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MessageApiService {
    @POST("api/messages/send")
    suspend fun sendMessage(
        @Body payload: RequestBody
    ): Response<Unit>
}
