package com.hurra.s2s

import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.lang.reflect.Type

/**
 * Network client for making API requests
 */
object NetworkClient {
    private const val TAG = "HurraS2SSDK"
    private const val BASE_URL = "https://s2s.hurra.com/"
    
    // Force debug logging for tests
    private val isDebug = BuildConfig.DEBUG || isTestEnvironment()
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (isDebug) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private var defaultUserAgent: String? = null
    
    /**
     * Initialize the NetworkClient with the application context
     * @param context The application context
     */
    fun initialize(context: Context) {
        defaultUserAgent = WebSettings.getDefaultUserAgent(context)
        if (isDebug) {
            Log.d(TAG, "Default User-Agent: $defaultUserAgent")
        }
    }
    
    private val userAgentInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Only add User-Agent if we have initialized it
        val newRequest = if (defaultUserAgent != null) {
            originalRequest.newBuilder()
                .header("User-Agent", defaultUserAgent!!)
                .build()
        } else {
            originalRequest
        }
        
        chain.proceed(newRequest)
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(userAgentInterceptor)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    
    private val apiService = retrofit.create(ApiService::class.java)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Make a POST request to the API
     * @param url The URL to post to
     * @param headers The headers to include in the request
     * @param body The body of the request
     * @return The response from the API
     */
    suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: Map<String, Any>
    ): EventResponse {
        return try {
            // Create a type for Map<String, Any>
            val mapType: Type = Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Any::class.java
            )
            val adapter = moshi.adapter<Map<String, Any>>(mapType)
            
            // Serialize the body to JSON
            val jsonBody = adapter.toJson(body)
            val requestBody = jsonBody.toRequestBody(jsonMediaType)
            
            // Log the request body in debug mode
            if (isDebug) {
                Log.d(TAG, "Request URL: $url")
                Log.d(TAG, "Request headers: $headers")
                Log.d(TAG, "Request body: $jsonBody")
            }
            
            val response = apiService.postEvent(url, headers, requestBody)
            val responseBodyString = response.body()?.string() ?: response.errorBody()?.string()
            
            if (isDebug) {
                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Response body: $responseBodyString")
            }
            
            if (response.isSuccessful) {
                EventResponse(
                    success = true, 
                    statusCode = response.code(),
                    responseBody = responseBodyString
                )
            } else {
                Log.e(TAG, "API request failed with code: ${response.code()}")
                EventResponse(
                    success = false, 
                    statusCode = response.code(),
                    responseBody = responseBodyString
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making request", e)
            throw e
        }
    }
    
    /**
     * Check if we're running in a test environment
     */
    @Suppress("SENSELESS_COMPARISON")
    private fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.junit.Test") != null
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}

/**
 * Response from an event tracking request
 */
data class EventResponse(
    val success: Boolean,
    val statusCode: Int,
    val responseBody: String? = null
)