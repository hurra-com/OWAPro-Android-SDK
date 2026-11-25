package com.hurra.s2s

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrivacyPrefsAPIBaseUrlTest : BaseRobolectricTest() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun api(baseUrl: String? = null): PrivacyPrefsAPI =
        if (baseUrl == null) {
            PrivacyPrefsAPI(context = context, accountId = "acc", apiKey = "key", userId = "user")
        } else {
            PrivacyPrefsAPI(context = context, accountId = "acc", apiKey = "key", userId = "user", baseUrl = baseUrl)
        }

    @Test
    fun `consent API URLs with default base URL are unchanged`() {
        val api = api()
        assertEquals(
            "https://s2s.hurra.com/consent-api/acc/consentStatus?app=1&user_id=user",
            api.buildUrl("consentStatus", emptyMap())
        )
        assertEquals(
            "https://s2s.hurra.com/consent-api/acc/vendors/analytics?app=1&user_id=user&fields=a,b",
            api.buildUrl("vendors/analytics", mapOf("fields" to "a,b"))
        )
    }

    @Test
    fun `consent API URLs with custom base URL`() {
        val expectedRoot = "https://www.example.com/consent-api/acc/consentStatus?app=1&user_id=user"
        assertEquals(expectedRoot, api("https://www.example.com").buildUrl("consentStatus", emptyMap()))
        assertEquals(expectedRoot, api("https://www.example.com/").buildUrl("consentStatus", emptyMap()))

        val expectedPath = "https://www.example.com/hc_s2s/consent-api/acc/translations/en?app=1&user_id=user"
        assertEquals(expectedPath, api("https://www.example.com/hc_s2s").buildUrl("translations/en", emptyMap()))
        assertEquals(expectedPath, api("https://www.example.com/hc_s2s/").buildUrl("translations/en", emptyMap()))
    }

    @Test
    fun `non-https base URL is rejected`() {
        assertFailsWith<IllegalArgumentException> { api("http://www.example.com") }
    }
}
