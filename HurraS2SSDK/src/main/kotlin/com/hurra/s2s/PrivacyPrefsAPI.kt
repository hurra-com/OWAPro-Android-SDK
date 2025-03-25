package com.hurra.s2s

import android.content.Context
// import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CancellationException
//import retrofit2.Response
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * API for managing privacy preferences and communicating with the server
 */
class PrivacyPrefsAPI(
    private val context: Context,
    private val accountId: String,
    private val apiKey: String,
    private val userId: String,
    private val testing: Boolean? = false,
    private var privacyPrefs: PrivacyPrefs? = null,
) {
    private val TAG = "PrivacyPrefsAPI"
    private var BASE_URL = "https://s2s.hurra.com"
    private var showConsentBanner: Boolean? = null
    private var apiAvailable: Boolean? = null
    private var apiNotAvailableReason: String? = null
    
//    private val packageName: String = context.packageName
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    init {
        // Initialize NetworkClient with context to get default User-Agent
        NetworkClient.initialize(context)
        if (privacyPrefs == null) {
            privacyPrefs = PrivacyPrefs()
        }
    }

    internal fun setBaseUrl(url: String) {
        BASE_URL = url
    }

    /**
     * Set privacy preferences and send them to the server
     * @param prefs The privacy preferences to set
     * @return Result containing success/failure
     */
    fun setPrivacyPrefs(prefs: PrivacyPrefs) {
        privacyPrefs = prefs
//        return sendPrivacyPrefs()
    }

    /**
     * Get current privacy preferences
     * @return Current privacy preferences or null if not set
     */
    fun getPrivacyPrefs(): PrivacyPrefs? = privacyPrefs

    fun shouldShowConsentBanner(): Boolean {
        if (showConsentBanner == null) {
            return false
        }
        return showConsentBanner == true
    }

    private suspend fun prefetchConsentStatus() {
        try {
            val result = sendRequest<ConsentStatus>(
                    method = "GET",
                    endpoint = "consentStatus",
                    queryParams = mapOf(),
                    requestBody = mapOf()
            )
            if (result.isSuccess) {
                showConsentBanner = result.getOrNull()?.showConsentBanner
                apiAvailable = true
            } else {
                apiAvailable = false
                apiNotAvailableReason = result.exceptionOrNull()?.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prefetch consent status", e)
            apiAvailable = false
            apiNotAvailableReason = e.message
        }
    }

    private suspend fun checkApiAvailability(): Boolean {
        var response = false
        // Log.d(TAG, "checkApiAvailability: $apiAvailable")
        if (apiAvailable == null) {
            prefetchConsentStatus()
        }
        // Log.d(TAG, "checkApiAvailability 2: $apiAvailable")
        if (apiAvailable == true) {
            response = true
        }
        return response
    }

    suspend fun getConsentStatus(): Result<ConsentStatus> {
        if (!checkApiAvailability()) {
            return Result.failure(Exception(apiNotAvailableReason))
        }
        val result = sendRequest<ConsentStatus>(
            method = "GET",
            endpoint = "consentStatus",
            queryParams = mapOf(),
            requestBody = mapOf()
        )
//        Log.d(TAG, "getConsentStatus: $result")
        if (result.isSuccess) {
            // if (result.getOrNull()?.showConsentBanner != null) {
                showConsentBanner = result.getOrNull()?.showConsentBanner
            // }
            if (result.getOrNull()?.userPreferences != null) {
                privacyPrefs?.setUserPreferences(result.getOrNull()?.userPreferences)
            }
        }
        return result
    }

    suspend fun setConsentStatus(): Result<ConsentStatus> {
        if (!checkApiAvailability()) {
            return Result.failure(Exception(apiNotAvailableReason))
        }
        val result =  sendRequest<ConsentStatus>(
            method = "PUT",
            endpoint = "consentStatus",
            queryParams = mapOf(),
            requestBody = privacyPrefs?.toJson() as Map<String, Any>
        )
        if (result.isSuccess) {
            // if (result.getOrNull()?.showConsentBanner != null) {
                showConsentBanner = result.getOrNull()?.showConsentBanner
            // }
            if (result.getOrNull()?.userPreferences != null) {
                privacyPrefs?.setUserPreferences(result.getOrNull()?.userPreferences)
            }
        }
        return result
    }

    suspend fun getVendorsDetails(): Result<List<Vendor>> {
        if (!checkApiAvailability()) {
            return Result.failure(Exception(apiNotAvailableReason))
        }
        return sendRequestList<Vendor>(
            method = "GET",
            endpoint = "vendors",
            queryParams = mapOf(),
            requestBody = mapOf()
        )
    }

    enum class VendorType(val value: String) {
        VENDOR_ID("vendorId"),
        EXTERNAL_VENDOR_ID("externalVendorId")
    }

    suspend fun getVendorDetails(vendorId: String, vendorType: VendorType? = VendorType.VENDOR_ID): Result<Vendor> {
        if (!checkApiAvailability()) {
            return Result.failure(Exception(apiNotAvailableReason))
        }
        return sendRequest<Vendor>(
            method = "GET",
            endpoint = "vendor/${vendorType?.value}/$vendorId",
            queryParams = mapOf(),
            requestBody = mapOf()
        )
    }


    suspend fun getCategories(): Result<List<Category>> {
        if (!checkApiAvailability()) {
            return Result.failure(Exception(apiNotAvailableReason))
        }
        return sendRequestList<Category>(
            method = "GET",
            endpoint = "categories",
            queryParams = mapOf(),
            requestBody = mapOf()
        )
    }

    suspend fun getTranslations(language: String? = null, fields: List<String>? = null): Result<Translations> {
        if (!checkApiAvailability()) {
            return Result.failure(Exception(apiNotAvailableReason))
        }
        return sendRequest<Translations>(
            method = "GET",
            endpoint = "translations${if (language != null) "/$language" else ""}",
            queryParams = if (fields != null) mapOf("fields" to fields.joinToString(",")) else mapOf(),
            requestBody = mapOf()
        )
    }

    /**
     * Send a request to the API and parse the response to the specified type
     * @param method HTTP method (GET, POST, etc.)
     * @param endpoint API endpoint
     * @param queryParams Query parameters
     * @param requestBody Request body for POST requests
     * @return Result containing the parsed response or an exception
     */
    private suspend inline fun <reified T> sendRequest(
        method: String,
        endpoint: String,
        queryParams: Map<String, String>,
        requestBody: Map<String, Any>
    ): Result<T> {
        return try {
            // Build the URL with query parameters
            val queryString = if (queryParams.isEmpty()) "" else 
                "&" + queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            
            val url = "${BASE_URL}/consent-api/$accountId/$endpoint?app=1&user_id=${userId}${queryString}"
            
            // Prepare headers
            val headers = mutableMapOf<String, String>("Authorization" to "Bearer $apiKey")
            if (testing!!) {
                headers["Cookie"] = "tracking_devel_mode=1"
            }
            
            // Make the request
            val response = when (method) {
                "GET" -> NetworkClient.get(url, headers)
                "POST" -> NetworkClient.post(url, headers, requestBody)
                "PUT" -> NetworkClient.put(url, headers, requestBody)
                else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
            }
            
            // Log response for debugging
//            Log.d(TAG, "Response for $endpoint: Status=${response.statusCode}, Body=${response.responseBody}")
            
            if (response.success) {
                // Parse the response body
                val responseBody = response.responseBody ?: return Result.failure(Exception("Empty response body"))
                val adapter = moshi.adapter(T::class.java)
                try {
                    val parsedResponse = adapter.fromJson(responseBody)
                        ?: return Result.failure(Exception("Failed to parse response"))
                    
                    Result.success(parsedResponse)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse response as ${T::class.java.simpleName}", e)
                    
                    // Try to parse as error response
                    try {
                        val errorAdapter = moshi.adapter(ErrorResponse::class.java)
                        val errorResponse = errorAdapter.fromJson(responseBody)
                        
                        Result.failure(Exception("API error: ${errorResponse?.error ?: "Unknown error"}"))
                    } catch (e2: Exception) {
                        // If both parsings fail, return the original error
                        Log.e(TAG, "Failed to parse as ErrorResponse too", e2)
                        Result.failure(e)
                    }
                }
            } else {
                // Failed response - try to parse as error response
                try {
                    val errorAdapter = moshi.adapter(ErrorResponse::class.java)
                    val errorResponse = response.responseBody?.let { errorAdapter.fromJson(it) }
                    
                    Result.failure(Exception("API error: ${errorResponse?.error ?: "Unknown error"} (${response.statusCode})"))
                } catch (e: Exception) {
                    Result.failure(Exception("Request failed with status code: ${response.statusCode}"))
                }
            }
        } catch (e: CancellationException) {
            // Don't wrap CancellationException to allow proper coroutine cancellation
            Log.w(TAG, "Request was canceled", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error making request: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a request to the API and parse the response to the specified type
     * @param method HTTP method (GET, POST, etc.)
     * @param endpoint API endpoint
     * @param queryParams Query parameters
     * @param requestBody Request body for POST requests
     * @return Result containing the parsed response or an exception
     */
    private suspend inline fun <reified T> sendRequestList(
        method: String,
        endpoint: String,
        queryParams: Map<String, String>,
        requestBody: Map<String, Any>
    ): Result<List<T>> {
        return try {
            // Build the URL with query parameters
            val queryString = if (queryParams.isEmpty()) "" else 
                "&" + queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            
            val url = "${BASE_URL}/consent-api/$accountId/$endpoint?app=1&user_id=${userId}${queryString}"
            
            // Prepare headers
            val headers = mutableMapOf<String, String>("Authorization" to "Bearer $apiKey")
            if (testing!!) {
                headers["Cookie"] = "tracking_devel_mode=1"
            }
            
            // Make the request
            val response = when (method) {
                "GET" -> NetworkClient.get(url, headers)
                "POST" -> NetworkClient.post(url, headers, requestBody)
                "PUT" -> NetworkClient.put(url, headers, requestBody)
                else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
            }
            
            // Log response for debugging
//            Log.d(TAG, "Response for $endpoint: Status=${response.statusCode}, Body=${response.responseBody}")
            
            if (response.success) {
                // Parse the response body
                val responseBody = response.responseBody ?: return Result.failure(Exception("Empty response body"))

                val listType = Types.newParameterizedType(List::class.java, T::class.java)
                val adapter = moshi.adapter<List<T>>(listType)
                try {
                    val parsedResponse = adapter.fromJson(responseBody)
                        ?: return Result.failure(Exception("Failed to parse response"))
                    
                    Result.success(parsedResponse)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse response as ${T::class.java.simpleName}", e)
                    
                    // Try to parse as error response
                    try {
                        val errorAdapter = moshi.adapter(ErrorResponse::class.java)
                        val errorResponse = errorAdapter.fromJson(responseBody)
                        
                        Result.failure(Exception("API error: ${errorResponse?.error ?: "Unknown error"}"))
                    } catch (e2: Exception) {
                        // If both parsings fail, return the original error
                        Log.e(TAG, "Failed to parse as ErrorResponse too", e2)
                        Result.failure(e)
                    }
                }
            } else {
                // Failed response - try to parse as error response
                try {
                    val errorAdapter = moshi.adapter(ErrorResponse::class.java)
                    val errorResponse = response.responseBody?.let { errorAdapter.fromJson(it) }
                    
                    Result.failure(Exception("API error: ${errorResponse?.error ?: "Unknown error"} (${response.statusCode})"))
                } catch (e: Exception) {
                    Result.failure(Exception("Request failed with status code: ${response.statusCode}"))
                }
            }
        } catch (e: CancellationException) {
            // Don't wrap CancellationException to allow proper coroutine cancellation
            Log.w(TAG, "Request was canceled", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error making request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
}