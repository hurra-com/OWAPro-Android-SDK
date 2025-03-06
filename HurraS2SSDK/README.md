# Hurra S2S SDK

Android SDK for tracking events and views in your application.

## Features
- User identification (Advertiser ID or generated UUID)
- Privacy preferences management
- Event tracking with timestamps
- View tracking with referrer support
- Automatic user ID generation and storage
- Configurable privacy preferences
- Default User-Agent support

## Installation

Add repository to your app's build.gradle(.kts):

Generate GitHub token and add it to your ~/.gradle/gradle.properties file:
```properties
gpr.user=your_github_username
gpr.key=your_github_token
```

Using Kotlin DSL (build.gradle.kts):
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hurra-com/OWAPro-Android-SDK")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Using Groovy DSL (settings.gradle):
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hurra-com/OWAPro-Android-SDK")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Add the dependency to your app's build.gradle(.kts):

Using Kotlin DSL (build.gradle.kts):
```kotlin
dependencies {
    implementation("com.hurra.s2s:HurraS2SSDK:1.0.1")
}
```

Using Groovy DSL (build.gradle):
```groovy
dependencies {
    implementation 'com.hurra.s2s:HurraS2SSDK:1.0.1'
}
```

## Configuration

Add the following to your `local.properties` file:

```properties
HURRA_S2S_ACCOUNT_ID=your_account_id
HURRA_S2S_API_KEY=your_api_key
```

## Usage

### Initialize SDK using auto-generated user ID

```kotlin
val sdk = HurraS2SSDK(
    context = context,
    accountId = BuildConfig.HURRA_S2S_ACCOUNT_ID,  // From local.properties
    apiKey = BuildConfig.HURRA_S2S_API_KEY,        // From local.properties
    useAdvertiserId = false         // Optional, defaults to false
)
```

### Initialize SDK using Advertiser ID

```kotlin
val sdk = HurraS2SSDK(
    context = context,
    accountId = BuildConfig.HURRA_S2S_ACCOUNT_ID,  // From local.properties
    apiKey = BuildConfig.HURRA_S2S_API_KEY,        // From local.properties
    useAdvertiserId = true
)
```

### Initialize SDK using custom user ID

```kotlin
val sdk = HurraS2SSDK(
    context = context,
    accountId = BuildConfig.HURRA_S2S_ACCOUNT_ID,  // From local.properties
    apiKey = BuildConfig.HURRA_S2S_API_KEY,        // From local.properties
    customUserId = "your_user_id"
)
```

### Privacy Preferences

The SDK supports detailed privacy preferences configuration through the PrivacyPrefs object:

```kotlin
val privacyPrefs = PrivacyPrefs().apply {
    // Global acceptance
    setAllCategories(true)  // Sets 'ac' or 'all' field

    // Individual categories
    setCategory(PrivacyPrefs.Category.ESSENTIAL, true)        // Sets 'c0'
    setCategory(PrivacyPrefs.Category.ANALYTICS, true)        // Sets 'c1'
    setCategory(PrivacyPrefs.Category.FUNCTIONAL, true)       // Sets 'c2'
    setCategory(PrivacyPrefs.Category.ADVERTISEMENT, false)   // Sets 'c3'
    setCategory(PrivacyPrefs.Category.PERSONALIZATION, false) // Sets 'c4'
    
    // Vendor preferences - multiple ways to identify vendors
    setVendorById("123", true)                // Sets '123' field
    setVendorByNameSlug("vendor-name", true)  // Sets 'v_vendor-name' field
    setVendorByExternalId("ext123", true)     // Sets 'x_ext123' field
}

// The resulting JSON will look like:
{
    "ac": 1,           // 1 = accept all, 0 = reject all
    "c0": 1,           // Essential category
    "c1": 1,           // Analytics category
    "c2": 1,           // Functional category
    "c3": 0,           // Advertisement category
    "c4": 0,           // Personalization category
    "123": 1,          // Vendor by ID
    "v_vendor-name": 1, // Vendor by name slug
    "x_ext123": 1      // Vendor by external ID
}

sdk.setPrivacyPrefs(privacyPrefs)
```

### Track Events

```kotlin
lifecycleScope.launch {
    sdk.trackEvent(
        eventType = "purchase",
        eventData = mapOf(
            "product_id" to "123",
            "value" to 99.99
        ),
        currentView = "checkout",
    ).onSuccess { response ->
        if (response.success) {
            Log.d("App", "Event tracked successfully")
        } else {
            Log.e("App", "Event tracking failed: ${response.responseBody}")
        }
    }
}
```

### Track Views

```kotlin
lifecycleScope.launch {
    sdk.trackView(
        eventData = mapOf(
            "page_type" to "product",
            "product_id" to "123"
        ),
        currentView = "product_detail"
    )
}
```

## URL Format

- If currentView is a valid URL, it will be used as-is
- If currentView is not a URL, it will be transformed to: `android-app://${application_package}/${currentView}`

## Request Format

All requests include:
- `event_ts`: Timestamp of the event
- `user_id`: Generated UUID or Advertiser ID
- `url`: Current view name/URL (transformed if needed)
- `referrer`: Previous view name/URL (if any)
- `is_interactive`: 1 if user interaction, 0 if not, omitted if null
- `privacy_prefs`: Privacy preferences (if set)
- Additional event data as provided

Requests are sent to: `https://s2s.hurra.com/rt/?cid=account_id&app=1`

Headers:
- `Authorization: Bearer your-api-key`
- `User-Agent`: Device's default User-Agent

## Response Format

```json
{
    "success": true,    // true for success, false for failure
    "statusCode": 200,  // HTTP status code
    "responseBody": ""  // Response body if any
}
```

## Debug Logging

Debug builds include detailed logging:
- Request URLs and headers
- Request and response bodies
- Network errors and exceptions
- SDK initialization details
- Default User-Agent information

## Requirements

- Android API level 24+
- Kotlin 1.8+
- AndroidX

## Error Handling

The SDK uses Kotlin's Result type:
```kotlin
sdk.trackEvent(...).onSuccess { response ->
    // Handle success
    // response.success: true for success, false for failure
    // response.statusCode: HTTP status code
    // response.responseBody: Response body if any
}.onFailure { error ->
    // Handle network or other errors
}
```

## Thread Safety

All network operations are suspend functions and should be called from a coroutine scope.

## Proguard/R8

If you're using proguard, add these rules to your proguard-rules.pro:

```proguard
-keep class com.hurra.s2s.** { *; }
-keepclassmembers class com.hurra.s2s.** { *; }
```

## License

[Add License Information]

## Running Integration Tests

To run integration tests, you need to provide test credentials:

1. Copy `local.properties.sample` to `local.properties`
2. Edit `local.properties` and add your test account ID and API key:
   ```
   test.accountId=your_test_account_id
   test.apiKey=your_test_api_key
   ```
3. Run the integration tests:
   ```
   ./gradlew test
   ```

If the credentials file doesn't exist or doesn't contain valid credentials, the integration tests will be skipped.
