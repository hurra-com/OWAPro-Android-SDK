package com.hurra.s2s

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CancellationException
import java.util.UUID
import android.webkit.URLUtil

/**
 * Hurra S2S SDK for tracking events and views
 * @param context Android context
 * @param accountId Account ID for authentication
 * @param apiKey API key for authentication
 * @param useAdvertiserId Whether to use advertiser ID or generate a unique ID
 * @param customUserId Optional custom user ID to use for tracking
 * @param testing Whether to enable testing mode
 * @param maxRetries Maximum number of retries for failed requests (default: 2)
 */
class HurraS2SSDK(
    private val context: Context,
    private val accountId: String,
    private val apiKey: String,
    private val useAdvertiserId: Boolean,
    private val customUserId: String? = null,
    private val testing: Boolean = false,
    private val maxRetries: Int = 2
) {
    private val TAG = "HurraS2SSDK"
    private var userId: String = ""
    private var privacyPrefs: PrivacyPrefs? = null
    private var previousView: String? = null
    private val packageName: String = context.packageName
    
    init {
        // Initialize NetworkClient with context to get default User-Agent
        NetworkClient.initialize(context)
        
        userId = if (useAdvertiserId) {
            // Use Android Advertising ID
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: getUserId()
        } else {
            if (customUserId != null) {
                customUserId
            } else {
                // Generate or retrieve stored user ID
                getUserId()
            }
        }
        
        Log.d(TAG, "Initialized with user ID: $userId")
    }
    
    private fun getUserId(): String {
        // Get stored ID or generate new one
        val prefs = context.getSharedPreferences("hurra_s2s_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("user_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("user_id", id).apply()
        }
        return id
    }

    /**
     * Check if a URL is an Android app URL
     * @param url The URL to check
     * @return Whether the URL is an Android app URL
     */
    private fun isAndroidAppUrl(url: String): Boolean {
        return url.startsWith("android-app://")
    }

    private fun isValidUrl(url: String): Boolean {
        return isAndroidAppUrl(url) || URLUtil.isValidUrl(url)
    }

    /**
     * Transform a screen name into a proper app URL format if it's not already a URL
     * @param screenName The screen name to transform
     * @return The transformed URL
     */
    private fun transformToAppUrl(screenName: String): String {
        return if (isValidUrl(screenName)) {
            screenName
        } else {
            "android-app://$packageName/$screenName"
        }
    }
    
    fun setPrivacyPrefs(prefs: PrivacyPrefs) {
        this.privacyPrefs = prefs
    }
    
    /**
     * Track an event
     * @param eventType The type of event to track
     * @param eventData Additional data to include with the event
     * @param currentView The current view/screen name
     * @param isInteractive Whether the event was triggered by user interaction (null if not specified)
     * @return Result containing the response or an exception
     */
    suspend fun trackEvent(
        eventType: String,
        eventData: Map<String, Any> = emptyMap(),
        currentView: String = "",
        isInteractive: Boolean? = null
    ): Result<EventResponse> {
        // Transform currentView to app URL format if needed
        val formattedCurrentView = transformToAppUrl(currentView)
        
        // Create request body with base fields
        val requestBody = mutableMapOf<String, Any>(
            "event_type" to eventType,
            "user_id" to userId,
            "url" to formattedCurrentView,
            "event_ts" to (System.currentTimeMillis() / 1000).toInt(), // Unix timestamp in seconds
        )

        if (previousView != null) {
            var formattedPreviousView = transformToAppUrl(previousView!!)
            requestBody["referer"] = formattedPreviousView
        }

        // Only add is_interactive if it's not null
        if (isInteractive != null) {
            Log.d(TAG, "Tracking event with isInteractive: $isInteractive")
            requestBody["is_interactive"] = if (isInteractive) 1 else 0
        }

        if (privacyPrefs != null) {
            requestBody["privacy_prefs"] = privacyPrefs!!.toJson()
        }
        
        // Add all event data at the top level
        requestBody.putAll(eventData)
        formattedCurrentView.also { previousView = it }
        return sendRequest(requestBody)
    }
    
    /**
     * Track a view/screen
     * @param eventData Additional data to include with the event
     * @param currentView The current view/screen name
     * @return Result containing the response or an exception
     */
    suspend fun trackView(
        eventData: Map<String, Any> = emptyMap(),
        currentView: String
    ): Result<EventResponse> {
        return trackEvent(
            eventType = "page_view",
            eventData = eventData,
            currentView = currentView,
            isInteractive = true
        )
    }
    
    /**
     * Send a request to the API
     * @param requestBody The prepared request body
     * @param retryCount Current retry count (default: 0)
     * @return Result containing the response or an exception
     */
    private suspend fun sendRequest(
        requestBody: Map<String, Any>,
        retryCount: Int = 0
    ): Result<EventResponse> {
        return try {
            // Implementation of actual network request
            val headers = mutableMapOf<String, String>("Authorization" to "Bearer $apiKey")
            if (testing) {
                headers["Cookie"] = "tracking_devel_mode=1"
            }
            val response = NetworkClient.post(
                url = "https://s2s.hurra.com/rt/?cid=$accountId&app=1",
                headers = headers,
                body = requestBody
            )
            Result.success(response)
        } catch (e: CancellationException) {
            // Don't wrap CancellationException to allow proper coroutine cancellation
            Log.w(TAG, "Request was canceled", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error making request: ${e.message}", e)
            // Retry logic
            if (retryCount < maxRetries) {
                Log.d(TAG, "Retrying request (${retryCount + 1}/$maxRetries)")
                return sendRequest(requestBody, retryCount + 1)
            }
            Result.failure(e)
        }
    }
} 