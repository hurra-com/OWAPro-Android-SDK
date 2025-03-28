package com.hurra.s2s

import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import androidx.annotation.Keep
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
    private var BASE_URL = "https://s2s.hurra.com/"
    
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
    
    private var retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    
    private var apiService = retrofit.create(ApiService::class.java)
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

    // For testing purposes only
    internal fun overrideBaseUrl(url: String) {
        Log.d(TAG, "Overriding base URL to: $url")
        if (isTestEnvironment()) {
            BASE_URL = url
            // Recreate the retrofit instance with the new base URL
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
    }

    /**
     * Make a GET request to the API
     * @param url The URL to get from
     * @param headers The headers to include in the request
     * @return The response from the API
     */
    suspend fun get(
        url: String,
        headers: Map<String, String>
    ): EventResponse {
        return try {
            // Log the request in debug mode
            if (isDebug) {
                Log.d(TAG, "GET Request URL: $url")
                Log.d(TAG, "Request headers: $headers")
            }

            val response = apiService.getRequest(url, headers)
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
            Log.e(TAG, "Error making GET request", e)
            throw e
        }
    }

    /**
     * Make a PUT request to the API
     * @param url The URL to put to
     * @param headers The headers to include in the request
     * @param body The body of the request
     * @return The response from the API
     */
    suspend fun put(
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
                Log.d(TAG, "PUT Request URL: $url")
                Log.d(TAG, "Request headers: $headers")
                Log.d(TAG, "Request body: $jsonBody")
            }

            val response = apiService.putRequest(url, headers, requestBody)
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
            Log.e(TAG, "Error making PUT request", e)
            throw e
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

// consent api responses
data class ErrorResponse(
    val error: String
)

/* consentStatus */
data class ConsentStatus(
    val userPreferences: String?,
    val vendors: Map<String, Int>?,
    val externalVendors: Map<String, Int>?,
    val statusesReasons: Map<String, String>?,
    val acceptedVendorIds: List<String>?,
    val declinedVendorIds: List<String>?,
    val acceptedExternalVendorIds: List<String>?,
    val declinedExternalVendorIds: List<String>?,
    val showConsentBanner: Boolean?
)

@Keep
data class Vendor(
    val name: String?,
    val vendorId: String?,
    val externalVendorId: String?,
    val categoryName: String?,
    val legalBasis: String?,
    val defaultStatus: Int?
)

data class ExternalVendor(
    val name: List<String>?,
    val vendorId: List<String>?,
    val externalVendorId: String?,
    val categoryName: List<String>?,
    val legalBasis: List<String>?,
    val defaultStatus: List<Int>?
)

data class Category(
    val categoryName: String,
    val categoryId: Int,
    val vendorIds: List<String>,
    val externalVendorIds: List<String>?
)

data class Translations(
    val language: String?,
    val availableLanguages: List<String>?,
    val consentBar: ConsentBar?,
    val privacyCenter: PrivacyCenter?,
    val categories: List<CategoryInfo>?,
    val vendors: List<VendorInfo>?
)

data class ConsentBar(
    val header: String?,
    val text: String?,
    val bottomText: String?,
    val buttons: Map<String, ConsentBarButton>?,
    val cookiePolicy: ConsentBarPolicy?,
    val privacyPolicy: ConsentBarPolicy?
)

data class ConsentBarButton(
    val inline: Int?,
    val label: String?,
    val inlineToken: String?
)

data class ConsentBarPolicy(
    val inline: Int?,
    val label: String?,
    val url: String?,
    val inlineToken: String?
)

data class PrivacyCenter(
    val header: String?,
    val info: PrivacyCenterInfo?,
    val subHeader: String?,
    val buttons: Map<String, PrivacyCenterButton>?,
    val cookiePolicy: PrivacyCenterPolicy?,
    val privacyPolicy: PrivacyCenterPolicy?
)

data class PrivacyCenterInfo(
    val text: String?,
    val link: String?
)

data class PrivacyCenterButton(
    val label: String?,
    val url: String?
)

data class PrivacyCenterPolicy(
    val label: String?,
    val url: String?
)

data class CategoryInfo(
    val categoryId: Int?,
    val categoryName: String?,
    val label: String?,
    val description: String?
)

data class VendorInfo(
    val name: String?,
    val vendorId: String?,
    val externalVendorId: String?,
    val categoryName: String?,
    val description: String?,
    val cookiePolicy: String?,
    val privacyPolicy: String?,
    val optOut: String?
)

