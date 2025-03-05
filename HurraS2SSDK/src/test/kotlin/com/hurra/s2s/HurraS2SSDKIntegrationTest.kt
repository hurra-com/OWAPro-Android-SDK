package com.hurra.s2s

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import android.webkit.WebSettings
import android.webkit.URLUtil
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Assume

/**
 * Integration test for HurraS2SSDK that makes actual API requests.
 * Note: This test requires an internet connection and valid API credentials.
 */
class HurraS2SSDKIntegrationTest {
    
    companion object {
        init {
            // Mock Android Log class and redirect to println
//            println("Setting up test logger(1)")
            mockkStatic(Log::class)
            every { Log.v(any(), any<String>()) } answers { 
                println("VERBOSE: ${arg<String>(0)} - ${arg<String>(1)}")
                0
            }
            every { Log.d(any(), any<String>()) } answers { 
                println("DEBUG: ${arg<String>(0)} - ${arg<String>(1)}")
                0
            }
            every { Log.i(any(), any<String>()) } answers { 
                println("INFO: ${arg<String>(0)} - ${arg<String>(1)}")
                0
            }
            every { Log.w(any(), any<String>()) } answers { 
                println("WARN: ${arg<String>(0)} - ${arg<String>(1)}")
                0
            }
            every { Log.w(any(), any<Throwable>()) } answers {
                println("WARN: ${arg<String>(0)} - ${arg<Throwable>(1)}")
                arg<Throwable>(1).printStackTrace()
                0
            }
            every { Log.e(any(), any<String>()) } answers { 
                println("ERROR: ${arg<String>(0)} - ${arg<String>(1)}")
                0
            }
            every { Log.e(any(), any<String>(), any()) } answers { 
                println("ERROR: ${arg<String>(0)} - ${arg<String>(1)}")
                println("EXCEPTION: ${arg<Throwable>(2)}")
                arg<Throwable>(2).printStackTrace()
                0
            }
            
            // Mock WebSettings.getDefaultUserAgent
            mockkStatic(WebSettings::class)
            every { WebSettings.getDefaultUserAgent(any()) } returns "Mozilla/5.0 (Linux; Android 10; Test Device) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
            
            // Mock URLUtil.isValidUrl
            mockkStatic(URLUtil::class)
            every { URLUtil.isValidUrl(any()) } answers { 
                val url = arg<String>(0)
                url.startsWith("http://") || url.startsWith("https://") || url.startsWith("android-app://")
            }
        }
    }
    
    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var contentResolver: ContentResolver
    
    @Before
    fun setup() {
        // Set up logging redirection
        TestLogger.setup()
        
        // Skip all tests if credentials are not available
        Assume.assumeTrue(
            "Skipping integration tests: Test credentials not available",
            TestCredentials.hasRequiredCredentials()
        )
        
        context = mockk(relaxed = true)
        sharedPrefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        contentResolver = mockk()
        
        mockkStatic(Settings.Secure::class)
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { context.contentResolver } returns contentResolver
        every { context.packageName } returns "com.hurra.testapp"
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs
        every { 
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) 
        } returns "test_device_id_for_integration"
        
        // For user ID persistence
        every { sharedPrefs.getString("user_id", null) } returns "test_user_id"
    }
    
    @Test
    fun `test track event with real API call`() = runBlocking {
        // Given
        val accountId = TestCredentials.getAccountId()!!
        val apiKey = TestCredentials.getApiKey()!!
        
        val sdk = HurraS2SSDK(
            context = context,
            accountId = accountId,
            apiKey = apiKey,
            useAdvertiserId = false,
            testing = true
        )

        val privacyPrefs = PrivacyPrefs()
        privacyPrefs.setAllCategories(true)
        sdk.setPrivacyPrefs(privacyPrefs)
        
        // When
        val result = sdk.trackEvent(
            eventType = "test_event",
            eventData = mapOf("test_key" to "test_value"),
            currentView = "test_view",
            isInteractive = true
        )
        
        // Then
        assertTrue(result.isSuccess, "API call should succeed")
        val response = result.getOrNull()!!
        assertEquals(200, response.statusCode, "Status code should be 200")
        assertTrue(response.success, "Response should indicate success")
        println("Response body: ${response.responseBody}")
    }
    
    @Test
    fun `test track view with real API call`() = runBlocking {
        // Given
        val accountId = TestCredentials.getAccountId()!!
        val apiKey = TestCredentials.getApiKey()!!
        
        val sdk = HurraS2SSDK(
            context = context,
            accountId = accountId,
            apiKey = apiKey,
            useAdvertiserId = false,
            testing = true
        )

        val privacyPrefs = PrivacyPrefs()
        privacyPrefs.setAllCategories(true)
        sdk.setPrivacyPrefs(privacyPrefs)
        
        // When
        val result = sdk.trackView(
            eventData = mapOf("screen_name" to "test_screen"),
            currentView = "test_view"
        )
        
        // Then
        assertTrue(result.isSuccess, "API call should succeed")
        val response = result.getOrNull()!!
        assertEquals(200, response.statusCode, "Status code should be 200")
        assertTrue(response.success, "Response should indicate success")
        println("Response body: ${response.responseBody}")
    }
    
    @Test
    fun `test multiple events in sequence`() = runBlocking {
        // Skip this test if we're not in debug mode
        if (!BuildConfig.DEBUG) {
            println("Skipping integration test in non-debug build")
            return@runBlocking
        }
        
        // Create SDK with testing flag enabled
        val sdk = HurraS2SSDK(
            context = context,
            accountId = TestCredentials.getAccountId()!!,
            apiKey = TestCredentials.getApiKey()!!,
            useAdvertiserId = false,
            testing = true
        )

        val privacyPrefs = PrivacyPrefs()
        privacyPrefs.setAllCategories(true)
        sdk.setPrivacyPrefs(privacyPrefs)
        
        // Track a sequence of events
        val screens = listOf("home", "product_list", "product_detail", "cart", "checkout")
        
        for (screen in screens) {
            // First track a view
            val viewResult = sdk.trackView(
                eventData = mapOf("screen_name" to screen),
                currentView = screen
            )
            assertTrue(viewResult.isSuccess, "View tracking for $screen should succeed")
            
            // Then track an interaction
            val eventResult = sdk.trackEvent(
                eventType = "${screen}_interaction",
                eventData = mapOf("action" to "click"),
                currentView = screen,
                isInteractive = true
            )
            assertTrue(eventResult.isSuccess, "Event tracking for $screen should succeed")
            
            // Small delay to avoid rate limiting
            kotlinx.coroutines.delay(100)
        }
        
        println("Successfully sent sequence of events")
    }
    
    @Test
    fun `test URL transformation`() = runBlocking {
        // Given
        val accountId = TestCredentials.getAccountId()!!
        val apiKey = TestCredentials.getApiKey()!!
        
        val sdk = HurraS2SSDK(
            context = context,
            accountId = accountId,
            apiKey = apiKey,
            useAdvertiserId = false,
            testing = true
        )

        val privacyPrefs = PrivacyPrefs()
        privacyPrefs.setAllCategories(true)
        sdk.setPrivacyPrefs(privacyPrefs)
        
        // Test with a regular screen name
        val result1 = sdk.trackEvent(
            eventType = "test_event",
            eventData = mapOf("test_key" to "test_value"),
            currentView = "home_screen",
            isInteractive = true
        )
        assertTrue(result1.isSuccess, "API call with screen name should succeed")
        
        // Test with an already valid URL
        val result2 = sdk.trackEvent(
            eventType = "test_event",
            eventData = mapOf("test_key" to "test_value"),
            currentView = "https://example.com/page",
            isInteractive = true
        )
        assertTrue(result2.isSuccess, "API call with URL should succeed")
        
        println("URL transformation test passed")
    }
} 