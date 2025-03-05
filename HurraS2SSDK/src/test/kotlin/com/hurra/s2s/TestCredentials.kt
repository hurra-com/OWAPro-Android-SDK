package com.hurra.s2s

import java.io.File
import java.util.Properties

/**
 * Utility class to read test credentials from a local file
 */
object TestCredentials {
    private const val CREDENTIALS_FILE = "local.properties"
    private const val ACCOUNT_ID_KEY = "test.accountId"
    private const val API_KEY_KEY = "test.apiKey"
    
    private val properties: Properties by lazy {
        val props = Properties()
        
        // Try to find the credentials file in multiple locations
        val possibleLocations = listOf(
            // Current directory
            File(CREDENTIALS_FILE),
            // Project root directory
            File("../$CREDENTIALS_FILE"),
            // Module directory
            File("HurraS2SSDK/$CREDENTIALS_FILE"),
            // Absolute path from user home
            File(System.getProperty("user.home"), CREDENTIALS_FILE)
        )
        
        // Try each location until we find the file
        val file = possibleLocations.find { it.exists() }
        
        if (file != null) {
            if(BuildConfig.DEBUG) {
                println("Loading test credentials from ${file.absolutePath}")
            }
            file.inputStream().use { props.load(it) }
            
            // Log the loaded properties (without sensitive values)
            if(BuildConfig.DEBUG) {
                println("Loaded properties: ${props.keys.joinToString()}")
                println("Found test.accountId: ${!props.getProperty(ACCOUNT_ID_KEY).isNullOrBlank()}")
                println("Found test.apiKey: ${!props.getProperty(API_KEY_KEY).isNullOrBlank()}")
            }
        } else {
            if(BuildConfig.DEBUG) {
                println("Could not find credentials file in any of these locations:")
                possibleLocations.forEach { println("  - ${it.absolutePath}") }
            }
        }
        
        props
    }
    
    /**
     * Get the test account ID from the credentials file
     * @return The test account ID or null if not found
     */
    fun getAccountId(): String? = properties.getProperty(ACCOUNT_ID_KEY)
    
    /**
     * Get the test API key from the credentials file
     * @return The test API key or null if not found
     */
    fun getApiKey(): String? = properties.getProperty(API_KEY_KEY)
    
    /**
     * Check if the required test credentials are available
     * @return True if the required credentials are available, false otherwise
     */
    fun hasRequiredCredentials(): Boolean {
        val hasCredentials = !getAccountId().isNullOrBlank() && !getApiKey().isNullOrBlank()
        if (!hasCredentials) {
            println("Missing required credentials. Please check your local.properties file.")
        }
        return hasCredentials
    }
} 