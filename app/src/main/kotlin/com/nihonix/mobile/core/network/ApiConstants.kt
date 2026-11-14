package com.nihonix.mobile.core.network

import com.nihonix.mobile.BuildConfig

object ApiConstants {
    const val DEFAULT_BASE_URL = BuildConfig.API_BASE_URL

    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_NO_AUTH = "No-Authentication"
    const val BEARER_PREFIX = "Bearer "

    const val TIMEOUT_CONNECT_SECONDS = 30L
    const val TIMEOUT_READ_SECONDS = 30L
    const val TIMEOUT_WRITE_SECONDS = 30L
}
