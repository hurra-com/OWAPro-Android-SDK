package com.hurra.s2s

import java.net.URI
import java.net.URISyntaxException

/**
 * Validation and normalisation of the base URL that the SDK sends its requests to.
 */
internal object BaseUrl {

    /**
     * Validate [baseUrl] and return it without trailing slashes, so that paths can be
     * appended with a single "/".
     *
     * Accepted: `https://host`, `https://host/`, `https://host/path`, `https://host/path/`.
     * @throws IllegalArgumentException if the URL is not an absolute https URL with a host,
     * or if it contains a query or a fragment.
     */
    fun normalize(baseUrl: String): String {
        val uri = try {
            URI(baseUrl.trim())
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException("Invalid baseUrl: $baseUrl", e)
        }
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "baseUrl must use https: $baseUrl"
        }
        require(!uri.host.isNullOrEmpty()) {
            "baseUrl must contain a host: $baseUrl"
        }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "baseUrl must not contain a query or a fragment: $baseUrl"
        }
        return baseUrl.trim().trimEnd('/')
    }
}
