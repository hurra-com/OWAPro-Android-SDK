package com.hurra.s2s

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.webkit.URLUtil
import android.webkit.WebSettings
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import android.util.Log

class HurraS2SSDKTest {
    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var contentResolver: ContentResolver
    
    @Before
    fun setup() {
        context = mockk()
        sharedPrefs = mockk()
        editor = mockk()
        contentResolver = mockk()
        
        mockkStatic(Settings.Secure::class)
        mockkStatic(WebSettings::class)
        mockkObject(NetworkClient)
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { context.contentResolver } returns contentResolver
        every { context.packageName } returns "com.test.app"
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs
        
        // For user ID persistence
        every { sharedPrefs.getString("user_id", null) } returns "test_user_id"
        
        // Set up logging redirection
        TestLogger.setup()
        
        // Add this mock
        every { WebSettings.getDefaultUserAgent(any()) } returns "test-user-agent"

        // Mock URLUtil.isValidUrl
        mockkStatic(URLUtil::class)
        every { URLUtil.isValidUrl(any()) } answers {
            val url = arg<String>(0)
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("android-app://")
        }

        // Add default mock for NetworkClient.post
        coEvery { 
            NetworkClient.post(
                url = any(),
                headers = any(),
                body = any()
            )
        } returns EventResponse(success = true, statusCode = 200)
    }

    @Test
    fun `test initialization with custom user ID`() {
        // Given
        val sdk = HurraS2SSDK(
            context = context,
            accountId = "account_id",
            apiKey = "api_key",
            useAdvertiserId = false,
            customUserId = "custom_user_id"
        )
        
        // When/Then - verify the user ID is set correctly
        // This is a bit tricky to test directly since userId is private
        // We'll test it indirectly through the trackEvent method
        runBlocking {
            coEvery { 
                NetworkClient.post(
                    url = any(),
                    headers = any(),
                    body = match<Map<String, Any>> { it["user_id"] == "custom_user_id" }
                )
            } returns EventResponse(success = true, statusCode = 200)
            assertEquals("custom_user_id", sdk.getUserId())
            val result = sdk.trackEvent("test_event")
            assert(result.isSuccess)

            coVerify { 
                NetworkClient.post(
                    url = any(),
                    headers = any(),
                    body = match<Map<String, Any>> { it["user_id"] == "custom_user_id" }
                )
            }
        }
    }
    
    @Test
    fun `test initialization with advertiser ID`() {
        // Given
        every { 
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) 
        } returns "advertiser_id"

        val sdk = HurraS2SSDK(
            context = context,
            accountId = "account_id",
            apiKey = "api_key",
            useAdvertiserId = true
        )

        runBlocking {
            coEvery { 
                NetworkClient.post(
                    url = any(),
                    headers = any(),
                    body = match<Map<String, Any>> { it["user_id"] == "advertiser_id" }
                )
            } returns EventResponse(success = true, statusCode = 200)
            val result = sdk.trackEvent("test_event")
            assert(result.isSuccess)

            // Should now be using our mocked advertising ID
            val userId = sdk.getUserId()
            assertEquals("advertiser_id", userId)

            coVerify { 
                NetworkClient.post(
                    url = any(),
                    headers = any(),
                    body = match<Map<String, Any>> { it["user_id"] == "advertiser_id" }
                )
            }
        }
    }
    
    @Test
    fun `test initialization with generated user ID`() {
        // Given
        val sdk = HurraS2SSDK(
            context = context,
            accountId = "account_id",
            apiKey = "api_key",
            useAdvertiserId = false
        )
        
        // When/Then - verify the user ID is set correctly
        runBlocking {
            coEvery { 
                NetworkClient.post(
                    url = any(),
                    headers = any(),
                    body = match<Map<String, Any>> { it["user_id"] == "test_user_id" }
                )
            } returns EventResponse(success = true, statusCode = 200)

            val result = sdk.trackEvent("test_event")
            assert(result.isSuccess)
            
            coVerify { 
                NetworkClient.post(
                    url = any(),
                    headers = any(),
                    body = match<Map<String, Any>> { it["user_id"] == "test_user_id" }
                )
            }
        }
    }
    
    @Test
    fun `test track event`() = runBlocking {
        // Given
        val sdk = HurraS2SSDK(
            context = context,
            accountId = "account_id",
            apiKey = "api_key",
            useAdvertiserId = false
        )
        
        // When
        val result = sdk.trackEvent(
            eventType = "test_event",
            eventData = mapOf("test_key" to "test_value"),
            currentView = "test_view",
            isInteractive = true
        )
        
        // Then
        assert(result.isSuccess)
        assertEquals(200, result.getOrNull()?.statusCode)
        
        coVerify { 
            NetworkClient.post(
                url = match { it.contains("account_id") },
                headers = match { it["Authorization"] == "Bearer api_key" },
                body = match { body ->
                    body["event_type"] == "test_event" && 
                    body["url"]?.toString()?.startsWith("android-app://") == true &&
                    body["test_key"] == "test_value" &&
                    body["is_interactive"] == 1 &&
                    body.containsKey("event_ts") &&
                    body["user_id"] == "test_user_id"
                }
            )
        }
    }
    
    @Test
    fun `test track view`() = runBlocking {
        // Given
        val sdk = HurraS2SSDK(
            context = context,
            accountId = "account_id",
            apiKey = "api_key",
            useAdvertiserId = false
        )
        
        // When
        val result = sdk.trackView(
            eventData = mapOf("screen_name" to "test_screen"),
            currentView = "test_view"
        )
        
        // Then
        assert(result.isSuccess)
        assertEquals(200, result.getOrNull()?.statusCode)
        
        coVerify { 
            NetworkClient.post(
                url = match { it.contains("account_id") },
                headers = match { it["Authorization"] == "Bearer api_key" },
                body = match { body ->
                    body["event_type"] == "page_view" && 
                    body["url"]?.toString()?.startsWith("android-app://") == true &&
                    body["screen_name"] == "test_screen" &&
                    body["is_interactive"] == 1 &&
                    body["user_id"] == "test_user_id" &&
                    body.containsKey("event_ts")
                }
            )
        }
    }
    
    @Test
    fun `test testing mode adds debug header`() = runBlocking {
        // Given
        val sdk = HurraS2SSDK(
            context = context,
            accountId = "account_id",
            apiKey = "api_key",
            useAdvertiserId = false,
            testing = true
        )
        
        coEvery { 
            NetworkClient.post(
                url = any(),
                headers = any(),
                body = any()
            )
        } returns EventResponse(success = true, statusCode = 200)
        
        // When
        val result = sdk.trackEvent("test_event")
        assert(result.isSuccess)
        
        // Then
        coVerify { 
            NetworkClient.post(
                url = any(),
                headers = match { headers ->
                    headers["Authorization"] == "Bearer api_key" &&
                    headers["Cookie"] == "tracking_devel_mode=1"
                },
                body = any()
            )
        }
    }
} 