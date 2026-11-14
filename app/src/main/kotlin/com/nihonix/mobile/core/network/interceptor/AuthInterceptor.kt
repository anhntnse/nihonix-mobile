package com.nihonix.mobile.core.network.interceptor

import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.storage.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // If explicitly marked as public (no auth), strip marker header and proceed
        if (originalRequest.header(ApiConstants.HEADER_NO_AUTH) != null) {
            val newRequest = originalRequest.newBuilder()
                .removeHeader(ApiConstants.HEADER_NO_AUTH)
                .build()
            return chain.proceed(newRequest)
        }

        val accessToken = tokenStorage.getAccessToken()
        return if (!accessToken.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header(ApiConstants.HEADER_AUTHORIZATION, "${ApiConstants.BEARER_PREFIX}$accessToken")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
