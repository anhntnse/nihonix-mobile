package com.nihonix.mobile.core.network

object ApiConstants {
    /**
     * Default base URL for Android Emulator pointing to host loopback.
     * Port 3010 matches the backend development configuration.
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:3010/api/"

    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_NO_AUTH = "No-Authentication"
    const val BEARER_PREFIX = "Bearer "

    const val TIMEOUT_CONNECT_SECONDS = 30L
    const val TIMEOUT_READ_SECONDS = 30L
    const val TIMEOUT_WRITE_SECONDS = 30L
}
