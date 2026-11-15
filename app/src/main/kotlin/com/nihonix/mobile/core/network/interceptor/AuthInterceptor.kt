package com.nihonix.mobile.core.network.interceptor

import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import okhttp3.Interceptor
import okhttp3.Response

data class SessionRequest(val userId: String?)

class AuthInterceptor(
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage
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

        val expected = originalRequest.tag(SessionRequest::class.java)
        if (expected != null && expected.userId != userStorage.getUser()?.id) {
            throw java.io.IOException("Account changed before request was sent.")
        }
        val accessToken = tokenStorage.getAccessToken()
        return if (!accessToken.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header(ApiConstants.HEADER_AUTHORIZATION, "${ApiConstants.BEARER_PREFIX}$accessToken")
                .apply { expected?.userId?.let { header("X-Nihonix-Account", it) } }
                .tag(SessionRequest::class.java, SessionRequest(userStorage.getUser()?.id))
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
