package com.nihonix.mobile.core.network.authenticator

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.auth.data.dto.RefreshTokenResponseDto
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage,
    private val gson: Gson,
    private val baseUrl: String = ApiConstants.DEFAULT_BASE_URL
) : Authenticator {
    private val lock = Any()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS).callTimeout(45, TimeUnit.SECONDS).build()

    override fun authenticate(route: Route?, response: Response): Request? {
        val authorization = response.request.header(ApiConstants.HEADER_AUTHORIZATION) ?: return null
        if (response.request.url.encodedPath.endsWith("/auth/login") ||
            response.request.url.encodedPath.endsWith("/auth/refresh")) return null
        synchronized(lock) {
            val requestUser = response.request.tag(SessionRequest::class.java)
            if (requestUser != null && requestUser.userId != userStorage.getUser()?.id) return null
            val current = tokenStorage.getAccessToken() ?: return null
            if(requestUser?.sessionId!=null&&requestUser.sessionId!=com.nihonix.mobile.core.network.interceptor.accessSessionId(current))return null
            val refresh = tokenStorage.getRefreshToken()
            if (refresh.isNullOrBlank()) { clearSession(); return null }
            if (responseCount(response) >= 2) { clearSession(); return null }
            val failedToken = authorization.removePrefix(ApiConstants.BEARER_PREFIX)
            if (current != failedToken) return retry(response.request, current)

            return when (val outcome = performRefresh(refresh)) {
                is RefreshOutcome.Success -> {
                    // Credential replacement and logout cleanup share the same local storage monitor.
                    synchronized(userStorage) {
                        if (tokenStorage.getRefreshToken() != refresh) return null
                        if(requestUser?.sessionId!=null&&requestUser.sessionId!=com.nihonix.mobile.core.network.interceptor.accessSessionId(outcome.tokens.accessToken))return null
                        try {
                            tokenStorage.saveTokens(outcome.tokens.accessToken, outcome.tokens.refreshToken)
                            retry(response.request, outcome.tokens.accessToken)
                        } catch (_: Exception) { clearSession(); null }
                    }
                }
                RefreshOutcome.Expired -> {
                    if (tokenStorage.getRefreshToken() == refresh) clearSession()
                    null
                }
                RefreshOutcome.TemporaryFailure -> null
            }
        }
    }

    private fun retry(request: Request, token: String): Request =
        request.newBuilder().header(ApiConstants.HEADER_AUTHORIZATION, ApiConstants.BEARER_PREFIX + token).build()

    private fun performRefresh(token: String): RefreshOutcome = try {
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/auth/refresh")
            .post(gson.toJson(mapOf("refreshToken" to token)).toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        refreshClient.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) return RefreshOutcome.Expired
            if (!response.isSuccessful) return RefreshOutcome.TemporaryFailure
            val body = response.body?.string() ?: return RefreshOutcome.TemporaryFailure
            val type = object : TypeToken<ApiResponseDto<RefreshTokenResponseDto>>() {}.type
            val envelope: ApiResponseDto<RefreshTokenResponseDto> = gson.fromJson(body, type)
            val tokens = envelope.data
            if (envelope.success && tokens != null && !tokens.accessToken.isNullOrBlank() && !tokens.refreshToken.isNullOrBlank()) {
                RefreshOutcome.Success(tokens)
            } else if (envelope.statusCode in listOf(401, 403)) RefreshOutcome.Expired
            else RefreshOutcome.TemporaryFailure
        }
    } catch (_: Exception) { RefreshOutcome.TemporaryFailure }

    private fun clearSession() { tokenStorage.clearTokens(); userStorage.clearUser() }

    private fun responseCount(response: Response): Int {
        var count = 1
        var previous = response.priorResponse
        while (previous != null) { count++; previous = previous.priorResponse }
        return count
    }

    private sealed interface RefreshOutcome {
        data class Success(val tokens: RefreshTokenResponseDto) : RefreshOutcome
        data object Expired : RefreshOutcome
        data object TemporaryFailure : RefreshOutcome
    }
}
