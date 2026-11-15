package com.nihonix.mobile.core.network.interceptor

import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import okhttp3.Interceptor
import okhttp3.Response

data class SessionRequest(val userId: String?,val sessionId:String?=null)
fun accessSessionId(token:String?):String? = runCatching {
    val payload=String(java.util.Base64.getUrlDecoder().decode(token!!.split('.')[1]),Charsets.UTF_8)
    com.google.gson.JsonParser.parseString(payload).asJsonObject.get("sid").asString
}.getOrNull()

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
        if(expected?.sessionId!=null&&expected.sessionId!=accessSessionId(accessToken))throw java.io.IOException("Login session changed before request was sent.")
        return if (!accessToken.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header(ApiConstants.HEADER_AUTHORIZATION, "${ApiConstants.BEARER_PREFIX}$accessToken")
                .apply { expected?.userId?.let { header("X-Nihonix-Account", it) } }
                .tag(SessionRequest::class.java, SessionRequest(userStorage.getUser()?.id,expected?.sessionId))
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
