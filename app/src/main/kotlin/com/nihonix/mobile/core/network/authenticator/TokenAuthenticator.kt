package com.nihonix.mobile.core.network.authenticator

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.dto.ApiResponseDto
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

class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage,
    private val gson: Gson,
    private val baseUrl: String = ApiConstants.DEFAULT_BASE_URL
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops (e.g. if retry also fails with 401)
        if (responseCount(response) >= 3) {
            Log.w(TAG, "Max retry count reached for 401 response")
            return null
        }

        // Do not authenticate requests to login or refresh endpoints
        val requestUrl = response.request.url.toString()
        if (requestUrl.contains("auth/refresh") || requestUrl.contains("auth/login")) {
            return null
        }

        synchronized(lock) {
            val currentAccessToken = tokenStorage.getAccessToken()
            val requestToken = response.request.header(ApiConstants.HEADER_AUTHORIZATION)
                ?.removePrefix(ApiConstants.BEARER_PREFIX)

            // If another thread already refreshed the token, retry with the updated token
            if (currentAccessToken != null && currentAccessToken != requestToken) {
                return response.request.newBuilder()
                    .header(ApiConstants.HEADER_AUTHORIZATION, "${ApiConstants.BEARER_PREFIX}$currentAccessToken")
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                Log.w(TAG, "No refresh token available, clearing auth session")
                clearSession()
                return null
            }

            // Attempt synchronous refresh call
            val newTokens = performRefreshToken(refreshToken)
            if (newTokens != null) {
                tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                return response.request.newBuilder()
                    .header(ApiConstants.HEADER_AUTHORIZATION, "${ApiConstants.BEARER_PREFIX}${newTokens.accessToken}")
                    .build()
            } else {
                Log.w(TAG, "Token refresh failed or expired, clearing auth session")
                clearSession()
                return null
            }
        }
    }

    private fun performRefreshToken(refreshToken: String): RefreshTokenResponseDto? {
        return try {
            val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val refreshUrl = "${cleanBaseUrl}auth/refresh"

            val jsonBody = gson.toJson(mapOf("refreshToken" to refreshToken))
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(refreshUrl)
                .post(requestBody)
                .header(ApiConstants.HEADER_NO_AUTH, "true")
                .build()

            // Direct standalone OkHttpClient to avoid interceptor recursion
            val client = OkHttpClient.Builder().build()
            client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) {
                    Log.e(TAG, "Refresh HTTP failed with status: ${res.code}")
                    return null
                }

                val bodyString = res.body?.string() ?: return null
                val type = object : TypeToken<ApiResponseDto<RefreshTokenResponseDto>>() {}.type
                val envelope: ApiResponseDto<RefreshTokenResponseDto> = gson.fromJson(bodyString, type)

                if (envelope.success && envelope.data != null) {
                    envelope.data
                } else {
                    Log.e(TAG, "Refresh API returned error envelope: ${envelope.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
            null
        }
    }

    private fun clearSession() {
        tokenStorage.clearTokens()
        userStorage.clearUser()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
    }
}
