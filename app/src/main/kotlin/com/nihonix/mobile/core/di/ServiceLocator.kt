package com.nihonix.mobile.core.di

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nihonix.mobile.core.network.NetworkModule
import com.nihonix.mobile.core.storage.SecureTokenStorage
import com.nihonix.mobile.core.storage.SharedPreferencesUserStorage
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.auth.data.api.AuthApiService
import com.nihonix.mobile.feature.auth.data.repository.AuthRepositoryImpl
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import com.nihonix.mobile.feature.topics.data.api.TopicsLevelsApiService
import com.nihonix.mobile.feature.topics.data.repository.TopicsLevelsRepositoryImpl
import com.nihonix.mobile.feature.topics.domain.repository.TopicsLevelsRepository

@SuppressLint("StaticFieldLeak")
object ServiceLocator {

    private lateinit var applicationContext: Context

    val gson: Gson by lazy {
        GsonBuilder()
            .create()
    }

    val tokenStorage: TokenStorage by lazy {
        SecureTokenStorage(applicationContext)
    }

    val userStorage: UserStorage by lazy {
        SharedPreferencesUserStorage(applicationContext, gson)
    }

    val networkModule: NetworkModule by lazy {
        NetworkModule(
            tokenStorage = tokenStorage,
            userStorage = userStorage,
            gson = gson
        )
    }

    val authApiService: AuthApiService by lazy {
        networkModule.createService(AuthApiService::class.java)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            apiService = authApiService,
            tokenStorage = tokenStorage,
            userStorage = userStorage,
            gson = gson
        )
    }

    val topicsLevelsApiService: TopicsLevelsApiService by lazy {
        networkModule.createService(TopicsLevelsApiService::class.java)
    }

    val topicsLevelsRepository: TopicsLevelsRepository by lazy {
        TopicsLevelsRepositoryImpl(
            apiService = topicsLevelsApiService,
            userStorage = userStorage,
            tokenStorage = tokenStorage,
            gson = gson
        )
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}
