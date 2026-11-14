package com.nihonix.mobile.core.network

import com.google.gson.Gson
import com.nihonix.mobile.core.network.authenticator.TokenAuthenticator
import com.nihonix.mobile.core.network.interceptor.AuthInterceptor
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkModule(
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage,
    private val gson: Gson,
    private val baseUrl: String = ApiConstants.DEFAULT_BASE_URL
) {
    val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(tokenStorage)
        val tokenAuthenticator = TokenAuthenticator(tokenStorage, userStorage, gson, baseUrl)

        OkHttpClient.Builder()
            .connectTimeout(ApiConstants.TIMEOUT_CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT_READ_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT_WRITE_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
