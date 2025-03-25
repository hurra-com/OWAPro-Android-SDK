package com.hurra.s2s

import org.json.JSONObject

class PrivacyPrefs {
    private val preferences = mutableMapOf<String, Any>()
    private var userPreferences: String? = null
    // private var showConsentBanner: Boolean = false
    // private val vendors = mutableMapOf<String, Any>()
    // private val externalVendors = mutableMapOf<String, Any>()
    
    fun setAllCategories(accept: Boolean) {
        preferences["all"] = if (accept) 1 else 0
    }
    
    fun setCategory(category: Category, accept: Boolean) {
        preferences["c${category.ordinal}"] = if (accept) 1 else 0
    }
    
    fun setVendorById(vendorId: String, accept: Boolean) {
        preferences[vendorId] = if (accept) 1 else 0
    }
    
    fun setVendorByNameSlug(nameSlug: String, accept: Boolean) {
        preferences["v_$nameSlug"] = if (accept) 1 else 0
    }
    
    fun setVendorByExternalId(externalId: String, accept: Boolean) {
        preferences["x_$externalId"] = if (accept) 1 else 0
    }
    
    fun toJson(): Map<String, Any> {
        return preferences.toMap()
    }

    fun setUserPreferences(userPreferences: String?) {
        this.userPreferences = userPreferences
    }

    fun getUserPreferences(): String? {
        return userPreferences
    }
    
    enum class Category {
        ESSENTIAL,
        ANALYTICS,
        FUNCTIONAL,
        ADVERTISEMENT,
        PERSONALIZATION
    }
} 