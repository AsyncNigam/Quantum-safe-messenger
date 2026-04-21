package com.nigdroid.quantummessenger.network.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Fallback API service for sending messages via REST when WebSocket is unavailable.
 */
interface MessageApiService {
    /**
     * Sends a protobuf-serialized message payload.
     */
    @POST("messages/send")
    suspend fun sendMessage(
        @Body payload: RequestBody
    ): Response<Unit>
}
