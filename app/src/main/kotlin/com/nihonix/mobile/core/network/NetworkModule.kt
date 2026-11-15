package com.nihonix.mobile.core.network

import com.google.gson.Gson
import com.nihonix.mobile.core.network.authenticator.TokenAuthenticator
import com.nihonix.mobile.core.network.interceptor.AuthInterceptor
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import okhttp3.OkHttpClient
import com.nihonix.mobile.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkModule(
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage,
    private val gson: Gson,
    private val baseUrl: String = ApiConstants.DEFAULT_BASE_URL,
    private val catalogStore: com.nihonix.mobile.core.offline.CatalogStore? = null
) {
    init {
        ApiEndpoint.validate(baseUrl, allowDevelopmentHttp = BuildConfig.DEBUG)
    }

    val okHttpClient: OkHttpClient by lazy {

        val authInterceptor = AuthInterceptor(tokenStorage, userStorage)
        val tokenAuthenticator = TokenAuthenticator(tokenStorage, userStorage, gson, baseUrl)

        OkHttpClient.Builder()
            .connectTimeout(ApiConstants.TIMEOUT_CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT_READ_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT_WRITE_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .apply { catalogStore?.let { addInterceptor(com.nihonix.mobile.core.offline.CatalogCacheInterceptor(it, baseUrl)) } }
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

    fun <T> createLongRunningService(serviceClass: Class<T>): T = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient.newBuilder().readTimeout(70, TimeUnit.SECONDS).callTimeout(80, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create(gson)).build().create(serviceClass)

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
