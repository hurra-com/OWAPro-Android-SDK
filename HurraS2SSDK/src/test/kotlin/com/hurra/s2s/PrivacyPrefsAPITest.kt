package com.hurra.s2s

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import android.util.Log


class PrivacyPrefsAPITest : BaseRobolectricTest() {

    @Mock
    private lateinit var context: Context

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: PrivacyPrefsAPI

    private val testAccountId = "test-account"
    private val testApiKey = "test-api-key"
    private val testUserId = "test-user"
    private val testPackageName = "com.test.app"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Override the NetworkClient to use our mock web server
        val baseUrl = mockWebServer.url("/").toString()
        NetworkClient.overrideBaseUrl(baseUrl)

        // Mock context
        context = ApplicationProvider.getApplicationContext()
//        `when`(context.packageName).thenReturn(testPackageName)

        // Initialize API
        api = PrivacyPrefsAPI(
            context = context,
            accountId = testAccountId,
            apiKey = testApiKey,
            userId = testUserId,
            testing = true
        )
        api.setBaseUrl(baseUrl)


    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getConsentStatus returns success with valid response`() = runBlocking {

        val apiStatusResponse = """
            {
                "vendors": {"vendor1": 1, "vendor2": 0},
                "externalVendors": {"ext1": 1},
                "showConsentBanner": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(apiStatusResponse)
                .addHeader("Content-Type", "application/json")
        )
        // Prepare mock response
        val responseJson = """
            {
                "vendors": {"vendor1": 1, "vendor2": 0},
                "externalVendors": {"ext1": 1},
                "showConsentBanner": true
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        // Call the API
        val result = api.getConsentStatus()
        // Verify the request
        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(request)
        // Verify the result
        assertTrue(result.isSuccess)
        val consentStatus = result.getOrNull()
        assertNotNull(consentStatus)
        assertTrue(consentStatus.showConsentBanner ?: false)
        assertEquals(2, consentStatus.vendors?.size)
        assertTrue { api.shouldShowConsentBanner() == true }
    }

    @Test
    fun `getConsentStatus returns failure with error response`() = runBlocking {
        // Prepare mock error response
        val errorJson = """
            {
                "error": "Invalid account"
            }
        """.trimIndent()

        // mockWebServer.enqueue(
        //     MockResponse()
        //         .setResponseCode(400)
        //         .setBody(errorJson)
        //         .addHeader("Content-Type", "application/json")
        // )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(errorJson)
                .addHeader("Content-Type", "application/json")
        )

        // Call the API
        val result = api.getConsentStatus()

        // Verify the result
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message!!.contains("Invalid account"))
    }

    @Test
    fun `getVendorsDetails returns correctly parsed list`() = runBlocking {
        
        val apiStatusResponse = """
            {
                "vendors": {"vendor1": 1, "vendor2": 0},
                "externalVendors": {"ext1": 1},
                "showConsentBanner": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(apiStatusResponse)
                .addHeader("Content-Type", "application/json")
        )
        // Prepare mock response
        val responseJson = """
                 [
                    {
                        "name": "Vendor 1",
                        "vendorId": "v1",
                        "externalVendorId": "ext1",
                        "categoryName": "Analytics",
                        "legalBasis": "consent",
                        "defaultStatus": 1
                    },
                    {
                        "name": "Vendor 2",
                        "vendorId": "v2",
                        "externalVendorId": "ext2",
                        "categoryName": "Marketing",
                        "legalBasis": "legitimate interest",
                        "defaultStatus": 0
                    }
                ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        // Call the API
        val result = api.getVendorsDetails()

        // Verify the result
        assertTrue(result.isSuccess)
        val vendors = result.getOrNull()
        assertNotNull(vendors)
        // Log.d("PrivacyPrefsAPITest", "getVendors: $vendors")
        assertEquals(2, vendors.size)


       assertEquals("Vendor 1", vendors[0].name)
       assertEquals("v2", vendors[1].vendorId)
    }

    @Test
    fun `getVendorDetails returns correctly parsed list`() = runBlocking {
        
        val apiStatusResponse = """
            {
                "vendors": {"vendor1": 1, "vendor2": 0},
                "externalVendors": {"ext1": 1},
                "showConsentBanner": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(apiStatusResponse)
                .addHeader("Content-Type", "application/json")
        )
        // Prepare mock response
        val responseJson = """
            {
                "name": "Vendor 1",
                "vendorId": "v1",
                "externalVendorId": "ext1",
                "categoryName": "Analytics",
                "legalBasis": "consent",
                "defaultStatus": 1
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        // Call the API
        val result = api.getVendorDetails("v1")

        // Verify the result
        assertTrue(result.isSuccess)
        val vendor = result.getOrNull()
        assertNotNull(vendor)
        // Log.d("PrivacyPrefsAPITest", "getVendors: $vendors")
        assertEquals("Vendor 1", vendor.name)
        assertEquals("v1", vendor.vendorId)
        assertEquals("ext1", vendor.externalVendorId)
        assertEquals("Analytics", vendor.categoryName)
        assertEquals("consent", vendor.legalBasis)
        assertEquals(1, vendor.defaultStatus)
    }

    @Test
    fun `getExternalVendorDetails returns correctly parsed list`() = runBlocking {

        val apiStatusResponse = """
            {
                "vendors": {"vendor1": 1, "vendor2": 0},
                "externalVendors": {"ext1": 1},
                "showConsentBanner": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(apiStatusResponse)
                .addHeader("Content-Type", "application/json")
        )
        // Prepare mock response
        val responseJson = """
            {
                "name": ["Vendor 1"],
                "vendorId": ["v1"],
                "externalVendorId": "ext1",
                "categoryName": ["Analytics"],
                "legalBasis": ["consent"],
                "defaultStatus": [1]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        // Call the API
        val result = api.getExternalVendorDetails("v1")

        // Verify the result
        assertTrue(result.isSuccess)
        val vendor = result.getOrNull()
        assertNotNull(vendor)
//         Log.d("PrivacyPrefsAPITest", "getExternalVendorDetails: $vendor")
        assertEquals("Vendor 1",   vendor.name!![0])
        assertEquals("v1", vendor.vendorId!![0])
        assertEquals("ext1", vendor.externalVendorId)
        assertEquals("Analytics", vendor.categoryName!![0])
        assertEquals("consent", vendor.legalBasis!![0])
        assertEquals(1, vendor.defaultStatus!![0])
    }

   @Test
   fun `getCategories returns correctly parsed categories`() = runBlocking {

        val apiStatusResponse = """
            {
                "vendors": {"vendor1": 1, "vendor2": 0},
                "externalVendors": {"ext1": 1},
                "showConsentBanner": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(apiStatusResponse)
                .addHeader("Content-Type", "application/json")
        )
       // Prepare mock response
       val responseJson = """
           [
                {
                    "categoryName": "Analytics",
                    "categoryId": 1,
                    "vendorIds": ["v1", "v2"],
                    "externalVendorIds": ["ext1"]
                },
                {
                    "categoryName": "Marketing",
                    "categoryId": 2,
                    "vendorIds": ["v3"],
                    "externalVendorIds": ["ext2", "ext3"]
                }
            ]
       """.trimIndent()

       mockWebServer.enqueue(
           MockResponse()
               .setResponseCode(200)
               .setBody(responseJson)
               .addHeader("Content-Type", "application/json")
       )

       // Call the API
       val result = api.getCategories()

       // Verify the result
       assertTrue(result.isSuccess)
       val categories = result.getOrNull()
       assertNotNull(categories)
       assertEquals(2, categories.size)
       assertEquals("Analytics", categories[0].categoryName)
       assertEquals(2, categories[1].categoryId)
//       assertEquals(3, categories[1].externalVendorIds?.size ?: 0)
   }

    @Test
    fun `getTranslations returns correctly localized data`() = runBlocking {
        val apiStatusResponse = """
            {
                "vendors": {"vendor1": 1, "vendor2": 0},
                "externalVendors": {"ext1": 1},
                "showConsentBanner": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(apiStatusResponse)
                .addHeader("Content-Type", "application/json")
        )
        // Prepare mock response
        val responseJson = """
            {
                "language": "en",
                "availableLanguages": ["en", "de", "fr"],
                "consentBar": {
                    "header": "Privacy Notice",
                    "text": "We use cookies to improve your experience",
                    "buttons": {
                        "accept": {
                            "label": "Accept All"
                        },
                        "reject": {
                            "label": "Reject All"
                        }
                    }
                },
                "privacyCenter": {
                    "header": "Privacy Center"
                },
                "categories": [
                    {
                        "categoryId": 1,
                        "categoryName": "necessary",
                        "label": "Necessary",
                        "description": "Essential cookies"
                    }
                ],
                "vendors": [
                    {
                        "name": "Google Analytics",
                        "vendorId": "google",
                        "description": "Analytics service"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        // Call the API
        val result = api.getTranslations("en")

        // Verify the result
        assertTrue(result.isSuccess)
        val translations = result.getOrNull()
        assertNotNull(translations)
        assertEquals("en", translations.language)
        assertEquals(3, translations.availableLanguages?.size)
        assertEquals("Privacy Notice", translations.consentBar?.header)
        assertEquals("Necessary", translations.categories?.get(0)?.label)
    }

} 