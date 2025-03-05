package com.hurra.s2s

import org.json.JSONObject

class PrivacyPrefs {
    private val preferences = mutableMapOf<String, Any>()
    
    fun setAllCategories(accept: Boolean) {
        preferences["ac"] = if (accept) 1 else 0
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
    
    enum class Category {
        ESSENTIAL,
        ANALYTICS,
        FUNCTIONAL,
        ADVERTISEMENT,
        PERSONALIZATION
    }
} 