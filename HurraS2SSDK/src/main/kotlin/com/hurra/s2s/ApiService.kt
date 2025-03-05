package com.hurra.s2s

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for Hurra S2S API
 */
interface ApiService {
    @POST
    @Headers("Content-Type: application/json")
    suspend fun postEvent(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Response<ResponseBody>
}

/**
 * Request body for API calls
 */
data class RequestBody(
    val event_type: String,
    val user_id: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val properties: Map<String, Any> = emptyMap()
) 