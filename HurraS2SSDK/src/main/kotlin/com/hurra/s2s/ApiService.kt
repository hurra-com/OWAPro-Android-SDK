package com.hurra.s2s

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface for API service
 */
interface ApiService {
    /**
     * POST event to API
     * @param url URL to post to
     * @param headers Headers to include in request
     * @param body Request body
     * @return Response from API
     */
    @POST
    @Headers("Content-Type: application/json")
    suspend fun postEvent(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Response<ResponseBody>

    /**
     * GET request to API
     * @param url URL to get from
     * @param headers Headers to include in request
     * @return Response from API
     */
    @GET
    suspend fun getRequest(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<ResponseBody>

    /**
     * PUT request to API
     * @param url URL to put to
     * @param headers Headers to include in request
     * @param body Request body
     * @return Response from API
     */
    @PUT
    @Headers("Content-Type: application/json")
    suspend fun putRequest(
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