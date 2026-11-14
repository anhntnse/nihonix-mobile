package com.nihonix.mobile.core.network

import java.net.URI

/** Endpoint policy shared by networking and development-only permission UI. */
object ApiEndpoint {
    private val developmentHosts = setOf("10.0.2.2", "localhost", "127.0.0.1")

    fun validate(baseUrl: String, allowDevelopmentHttp: Boolean) {
        val endpoint = runCatching { URI(baseUrl) }.getOrNull()
        require(endpoint != null && !endpoint.host.isNullOrBlank() &&
            endpoint.userInfo == null && endpoint.rawQuery == null &&
            endpoint.rawFragment == null && baseUrl.endsWith("/")
        ) { "API URL must be an absolute URL ending in / without credentials, query or fragment." }
        require(endpoint.scheme == "https" ||
            (allowDevelopmentHttp && endpoint.scheme == "http" && isLocalDevelopment(baseUrl))
        ) { "API requests require HTTPS; HTTP is allowed only for local debug connections." }
        require(allowDevelopmentHttp || !isLocalDevelopment(baseUrl)) {
            "Release builds require a deployed HTTPS API endpoint."
        }
    }

    fun isLocalDevelopment(baseUrl: String): Boolean =
        runCatching { URI(baseUrl).host?.lowercase() in developmentHosts }.getOrDefault(false)
}
