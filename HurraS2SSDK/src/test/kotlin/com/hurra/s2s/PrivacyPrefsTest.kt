package com.hurra.s2s

import org.junit.Test
import kotlin.test.assertEquals

class PrivacyPrefsTest {
    @Test
    fun `test privacy prefs json serialization`() {
        // Given
        val prefs = PrivacyPrefs()
        
        // When
        prefs.setAllCategories(true)
        prefs.setCategory(PrivacyPrefs.Category.ANALYTICS, true)
        prefs.setVendorById("123", true)
        prefs.setVendorByNameSlug("test-vendor", false)
        prefs.setVendorByExternalId("ext123", true)
        
        // Then
        val json = prefs.toJson()
        assertEquals(1, json["all"])
        assertEquals(1, json["c1"])  // ANALYTICS
        assertEquals(1, json["123"])
        assertEquals(0, json["v_test-vendor"])
        assertEquals(1, json["x_ext123"])
    }
} 