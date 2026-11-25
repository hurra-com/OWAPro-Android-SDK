package com.hurra.s2s

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaseUrlTest {

    @Test
    fun `normalize keeps a base URL without trailing slash`() {
        assertEquals("https://s2s.hurra.com", BaseUrl.normalize("https://s2s.hurra.com"))
    }

    @Test
    fun `normalize removes trailing slashes`() {
        assertEquals("https://www.example.com", BaseUrl.normalize("https://www.example.com/"))
        assertEquals("https://www.example.com/hc_s2s", BaseUrl.normalize("https://www.example.com/hc_s2s/"))
        assertEquals("https://www.example.com/hc_s2s", BaseUrl.normalize("https://www.example.com/hc_s2s//"))
    }

    @Test
    fun `normalize rejects non-https URLs`() {
        assertFailsWith<IllegalArgumentException> { BaseUrl.normalize("http://www.example.com") }
        assertFailsWith<IllegalArgumentException> { BaseUrl.normalize("www.example.com") }
        assertFailsWith<IllegalArgumentException> { BaseUrl.normalize("") }
    }

    @Test
    fun `normalize rejects URLs without host, with query or fragment, or malformed`() {
        assertFailsWith<IllegalArgumentException> { BaseUrl.normalize("https:///hc_s2s") }
        assertFailsWith<IllegalArgumentException> { BaseUrl.normalize("https://www.example.com/hc_s2s?a=1") }
        assertFailsWith<IllegalArgumentException> { BaseUrl.normalize("https://www.example.com/hc_s2s#a") }
        assertFailsWith<IllegalArgumentException> { BaseUrl.normalize("https://www.example.com/hc s2s") }
    }

    @Test
    fun `tracking URL with default base URL is unchanged`() {
        assertEquals(
            "https://s2s.hurra.com/rt/?cid=<ACCOUNT_ID>&app=1",
            HurraS2SSDK.buildTrackingUrl(HurraS2SSDK.DEFAULT_BASE_URL, "<ACCOUNT_ID>")
        )
    }

    @Test
    fun `tracking URL with custom base URL`() {
        val expectedRoot = "https://www.example.com/rt/?cid=acc&app=1"
        assertEquals(expectedRoot, HurraS2SSDK.buildTrackingUrl("https://www.example.com", "acc"))
        assertEquals(expectedRoot, HurraS2SSDK.buildTrackingUrl("https://www.example.com/", "acc"))

        val expectedPath = "https://www.example.com/hc_s2s/rt/?cid=acc&app=1"
        assertEquals(expectedPath, HurraS2SSDK.buildTrackingUrl("https://www.example.com/hc_s2s", "acc"))
        assertEquals(expectedPath, HurraS2SSDK.buildTrackingUrl("https://www.example.com/hc_s2s/", "acc"))
    }
}
