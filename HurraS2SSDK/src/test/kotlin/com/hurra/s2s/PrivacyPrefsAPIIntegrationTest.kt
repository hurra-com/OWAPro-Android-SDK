package com.hurra.s2s

import android.content.ContentResolver
import android.content.Context
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
import java.util.UUID

/**
 * Integration test for PrivacyPrefsAPI that makes actual API requests.
 * Note: This test requires an internet connection and valid API credentials.
 */
class PrivacyPrefsAPIIntegrationTest {

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
    // private lateinit var sharedPrefs: SharedPreferences
    // private lateinit var editor: SharedPreferences.Editor
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
    }

    @Test
    fun testPrivacyPrefsAPI() = runBlocking {
        val accountId = TestCredentials.getAccountId()!!
        val apiKey = TestCredentials.getApiKey()!!

        val userId = UUID.randomUUID().toString()
        val privacyPrefsAPI = PrivacyPrefsAPI(
            context = context,
            accountId = accountId,
            apiKey = apiKey,
            userId = userId,
            testing = true
        )

        val result = privacyPrefsAPI.getConsentStatus()
        assertTrue(result.isSuccess, "API call should succeed")

        val response = result.getOrNull()!!
        assertNotNull(response)
        assertTrue(response.showConsentBanner == true)
        // val pprefs = privacyPrefsAPI.getPrivacyPrefs()
        // Log.d("PrivacyPrefsAPIIntegrationTest", "pprefs: $pprefs")
        assertTrue(response.showConsentBanner == privacyPrefsAPI.shouldShowConsentBanner())
        assertTrue(response.userPreferences == null)
        assertTrue(response.userPreferences == privacyPrefsAPI.getPrivacyPrefs()?.getUserPreferences())
        assertTrue(response.vendors?.size!! > 0)
        // assertTrue(response.externalVendors?.size!! > 0)
        assertTrue(response.statusesReasons != null)
        assertTrue(response.acceptedVendorIds != null)
        assertTrue(response.declinedVendorIds != null)
     
        // Test setConsentStatus
        val pprefs = privacyPrefsAPI.getPrivacyPrefs()
        pprefs?.setAllCategories(true)
        val result2 = privacyPrefsAPI.setConsentStatus()
        assertTrue(result2.isSuccess, "API call should succeed")
        val response2 = result2.getOrNull()!!
        // Log.d("PrivacyPrefsAPIIntegrationTest", "Response2: $response2")
        assertTrue(response2.showConsentBanner == null)
        assertTrue(privacyPrefsAPI.shouldShowConsentBanner() == false)
        assertTrue(response2.userPreferences != null)
        assertTrue(response2.userPreferences == pprefs?.getUserPreferences())
        assertTrue(response2.vendors?.size!! > 0)
        assertTrue(response2.statusesReasons != null)
        assertTrue(response2.acceptedVendorIds != null)
        assertTrue(response2.acceptedVendorIds?.size!! > 0)
        assertTrue(response2.declinedVendorIds != null)
        assertTrue(response2.declinedVendorIds?.size!! == 0)


        val testVendorId = TestCredentials.getVendorId()!!
        // Test getVendors
        val vendors = privacyPrefsAPI.getVendorsDetails()
        assertTrue(vendors.isSuccess, "API call should succeed")
        val vendorsResponse = vendors.getOrNull()!!
        assertTrue(vendorsResponse.size > 0)
        if (testVendorId != null) {
            assertTrue(vendorsResponse.any { it.vendorId == testVendorId }, "Vendor $testVendorId not found in response")
        }
        

        if (testVendorId != null) {
            // Test getVendor
            val vendor = privacyPrefsAPI.getVendorDetails(testVendorId)
            assertTrue(vendor.isSuccess, "API call should succeed")
            val vendorResponse = vendor.getOrNull()!!
            assertTrue(vendorResponse.vendorId == testVendorId)
            assertTrue(vendorResponse.name != null)
            assertTrue(vendorResponse.defaultStatus != null)
            assertTrue(vendorResponse.categoryName != null)
            assertTrue(vendorResponse.legalBasis != null)
        }

        val externalVendorId = TestCredentials.getExternalVendorId()!!
        if (externalVendorId != null) {
            val result3 = privacyPrefsAPI.getVendorDetails(externalVendorId, PrivacyPrefsAPI.VendorType.EXTERNAL_VENDOR_ID)
            assertTrue(result3.isSuccess, "API call should succeed")
            val vendorResponse3 = result3.getOrNull()!!
            assertTrue(vendorResponse3.vendorId != null)
            assertTrue(vendorResponse3.externalVendorId == externalVendorId)
            assertTrue(vendorResponse3.name != null)
            assertTrue(vendorResponse3.defaultStatus != null)
            assertTrue(vendorResponse3.categoryName != null)
        }

        val result4 = privacyPrefsAPI.getCategories()
        assertTrue(result4.isSuccess, "API call should succeed")
        val categoriesResponse = result4.getOrNull()!!
        assertTrue(categoriesResponse.size > 0)
        assertTrue(categoriesResponse.any { it.categoryId == 0 })
        assertTrue(categoriesResponse.any { it.categoryName == "ESSENTIAL" })
        assertTrue(categoriesResponse.all { it.vendorIds != null })
        assertTrue(categoriesResponse.all { it.vendorIds?.size!! > 0 })

        val result5 = privacyPrefsAPI.getTranslations()
        assertTrue(result5.isSuccess, "API call should succeed")
        val translationsResponse = result5.getOrNull()!!
        assertTrue(translationsResponse.language != null)
        assertTrue(translationsResponse.availableLanguages != null)
        assertTrue(translationsResponse.availableLanguages?.size!! > 0)
        assertTrue(translationsResponse.consentBar != null)
        assertTrue(translationsResponse.privacyCenter != null)
        assertTrue(translationsResponse.categories != null)
        assertTrue(translationsResponse.categories?.size!! > 0)

        val otherLanguage = if(translationsResponse.availableLanguages?.size!! > 1) translationsResponse.availableLanguages?.get(1) else null
        if (otherLanguage != null) {
            val result6 = privacyPrefsAPI.getTranslations(language = otherLanguage)
            assertTrue(result6.isSuccess, "API call should succeed")
            val translationsResponse2 = result6.getOrNull()!!
            assertTrue(translationsResponse2.language == otherLanguage)
        }

        val result7 = privacyPrefsAPI.getTranslations(fields = listOf("consentBar"))
        assertTrue(result7.isSuccess, "API call should succeed")
        val translationsResponse3 = result7.getOrNull()!!
        assertTrue(translationsResponse3.consentBar != null)
        assertTrue(translationsResponse3.privacyCenter == null)
        assertTrue(translationsResponse3.categories == null)
    }

}