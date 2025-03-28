# PrivacyPrefsAPI Documentation

## Overview

PrivacyPrefsAPI provides an interface for managing user privacy preferences and consent in Android applications using Hurra's S2S API. It communicates with Hurra's server to fetch and update consent status, vendor information, categories, and translations.

## Initialization

```kotlin
PrivacyPrefsAPI(
    context: Context,
    accountId: String,
    apiKey: String,
    userId: String,
    testing: Boolean? = false,
    privacyPrefs: PrivacyPrefs? = null
)
```
Parameters:
* context: Android context for network operations
* accountId: Your Hurra account identifier
* apiKey: Authentication API key for the Hurra service
* userId: Unique identifier for the current user
* testing: Optional flag to enable testing mode (adds special headers), defaults to false
* privacyPrefs: Optional initial privacy preferences, defaults to new instance if null

> [!IMPORTANT]
> userId should match the userid used in the HurraS2SSDK.
> If neither customUserId nor useAdvertiserId is used in the HurraS2SSDK, the return value of getUserId() should be passed as userId.

```kotlin
val sdk = HurraS2SSDK(
    context = context,
    accountId = BuildConfig.HURRA_S2S_ACCOUNT_ID,  // From local.properties
    apiKey = BuildConfig.HURRA_S2S_API_KEY        // From local.properties
)
val userId = sdk.getUserId()
val api = PrivacyPrefsAPI(
    context = context,
    accountId = BuildConfig.HURRA_S2S_ACCOUNT_ID,  // From local.properties
    apiKey = BuildConfig.HURRA_S2S_API_KEY,        // From local.properties
    userId = userId
)
```

# Core Methods

## Privacy Preferences

```kotlin
fun setPrivacyPrefs(prefs: PrivacyPrefs)
```
Sets privacy preferences locally.

```kotlin
fun getPrivacyPrefs(): PrivacyPrefs?
```
Returns current privacy preferences or null if not set.


## Consent Management

```kotlin
suspend fun getConsentStatus(): Result<ConsentStatus>
```
Fetches the consent status from the server. Updates local showConsentBanner flag and user preferences on success.

```kotlin
suspend fun setConsentStatus(status: ConsentStatus): Result<ConsentStatus>
```
Sends the current privacy preferences to the server and updates local state based on the response.

```kotlin
fun shouldShowConsentBanner(): Boolean
```
Returns whether the consent banner should be displayed to the user.

## Vendor Information

```kotlin
suspend fun getVendorsDetails(): Result<List<Vendor>>
```
Returns a list of all vendors available in the system.

```kotlin
suspend fun getVendorDetails(vendorId: String): Result<Vendor>
```
Parameters:
* vendorId: Identifier for the vendor
Returns the vendor details for the given vendor ID.

```kotlin
suspend fun getExternalVendorDetails(vendorId: String): Result<ExternalVendor>
```
Parameters:
* vendorId: Identifier for the external vendor
Returns the external vendor details for the given vendor ID.


## Categories

```kotlin
suspend fun getCategories(): Result<List<Category>>
```
Retrieves all available consent categories.

## Translations

```kotlin
suspend fun getTranslations(language: String? = null, fields: List<String>? = null): Result<Translations>
```
Fetches translations for consent UI elements.
Parameters:
* language: Optional language code, if ommited the default language will be used
* fields: Optional list of specific fields to translate

## response types

### ConsentStatus

```kotlin
data class ConsentStatus(
    val userPreferences: String?,
    val vendors: Map<String, Int>?,
    val externalVendors: Map<String, Int>?,
    val statusesReasons: Map<String, String>?,
    val acceptedVendorIds: List<String>?,
    val declinedVendorIds: List<String>?,
    val acceptedExternalVendorIds: List<String>?,
    val declinedExternalVendorIds: List<String>?,
    val showConsentBanner: Boolean?
)   
```

### Vendor

```kotlin
data class Vendor(
    val name: String?,
    val vendorId: String?,
    val externalVendorId: String?,
    val categoryName: String?,
    val legalBasis: String?,
    val defaultStatus: Int?
)
```

### ExternalVendor

```kotlin
data class ExternalVendor(
    val name: List<String>?,
    val vendorId: List<String>?,
    val externalVendorId: String?,
    val categoryName: List<String>?,
    val legalBasis: List<String>?,
    val defaultStatus: List<Int>?
)
```
### Category

```kotlin
data class Category(
    val categoryName: String,
    val categoryId: Int,
    val vendorIds: List<String>,
    val externalVendorIds: List<String>?
)
```

### Translations

```kotlin
data class Translations(
    val language: String?,
    val availableLanguages: List<String>?,
    val consentBar: ConsentBar?,
    val privacyCenter: PrivacyCenter?,
    val categories: List<CategoryInfo>?,
    val vendors: List<VendorInfo>?
)   
```

### ConsentBar

```kotlin
data class ConsentBar(
    val header: String?,
    val text: String?,
    val bottomText: String?,
    val buttons: Map<String, ConsentBarButton>?,
    val cookiePolicy: ConsentBarPolicy?,
    val privacyPolicy: ConsentBarPolicy?
)
```

### ConsentBarButton

```kotlin
data class ConsentBarButton(
    val inline: Int?,
    val label: String?,
    val inlineToken: String?
)
```

### ConsentBarPolicy    

```kotlin
data class ConsentBarPolicy(
    val inline: Int?,
    val label: String?,
    val url: String?,
    val inlineToken: String?
)
```

### PrivacyCenter

```kotlin
data class PrivacyCenter(
    val header: String?,
    val info: PrivacyCenterInfo?,
    val subHeader: String?,
    val buttons: Map<String, PrivacyCenterButton>?,
    val cookiePolicy: PrivacyCenterPolicy?, 
    val privacyPolicy: PrivacyCenterPolicy?
)
```

### PrivacyCenterInfo

```kotlin
data class PrivacyCenterInfo(
    val text: String?,
    val link: String?
)
```

### PrivacyCenterButton

```kotlin
data class PrivacyCenterButton(
    val label: String?,
    val url: String?
)
```

### PrivacyCenterPolicy

```kotlin
data class PrivacyCenterPolicy(
    val label: String?,
    val url: String?
)
```

### CategoryInfo

```kotlin
data class CategoryInfo(
    val categoryId: Int?,
    val categoryName: String?,
    val label: String?,
    val description: String?
)
```

### VendorInfo

```kotlin
data class VendorInfo(
    val name: String?,
    val vendorId: String?,
    val externalVendorId: String?,
    val categoryName: String?,
    val description: String?,
    val cookiePolicy: String?,
    val privacyPolicy: String?,
    val optOut: String?
)
```


## Internal Configuration

```kotlin
internal fun setBaseUrl(url: String)
```
Changes the base URL for API requests (useful for testing or different environments).

## Error Handling

All network methods return Kotlin's Result type which contains either the successful response or an exception describing the failure. The API automatically handles error responses from the server.

## Testing Support

The API includes a testing parameter which, when set to true, adds special headers to requests enabling development/testing mode on the server.

## Usage Example

```kotlin
val api = PrivacyPrefsAPI(context, accountId, apiKey, userId, testing)

// Get consent status
var consentStatus = api.getConsentStatus()


// if consent banner should be shown
if (api.shouldShowConsentBanner()) {


// accept all categories
api.getPrivacyPrefs()?.setAllCategories(true)

// send consent status to the server
consentStatus = api.setConsentStatus()
}
```