package com.nihonix.mobile.feature.system

import android.content.SharedPreferences
import com.google.gson.Gson
import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface SystemApi {
    @GET("app/config")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun config(): Response<ApiResponseDto<AppConfig>>
    @GET("health")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun health(): Response<okhttp3.ResponseBody>
}
interface ConfigCache {
    fun read(): Pair<AppConfig, Long>?
    fun save(config: AppConfig, time: Long)
}
class PreferencesConfigCache(private val prefs: SharedPreferences, private val gson: Gson) : ConfigCache {
    override fun read(): Pair<AppConfig, Long>? = runCatching {
        val config = gson.fromJson(prefs.getString("config", null), AppConfig::class.java)?.validated()
        config?.let { it to prefs.getLong("fetched_at", 0) }
    }.getOrNull()
    override fun save(config: AppConfig, time: Long) {
        prefs.edit().putString("config", gson.toJson(config)).putLong("fetched_at", time).apply()
    }
}
class StartupRepository(
    private val api: SystemApi,
    private val cache: ConfigCache,
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend fun load(version: Long): StartupState = withContext(Dispatchers.IO) {
        try {
            // Health is diagnostic only; it must never independently block study.
            val response = api.config()
            check(response.isSuccessful)
            val config = requireNotNull(response.body()?.data).validated()
            runCatching { cache.save(config, now()) }
            StartupState(config.restriction(version), config)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            val cached = runCatching { cache.read() }.getOrNull()
            if (cached != null) {
                val (config, saved) = cached
                val restriction = config.restriction(version)
                val age = now() - saved
                if (restriction != StartupStatus.READY || age in 0..(config.cacheTtlSeconds!! * 1000))
                    return@withContext StartupState(restriction, config, "Unable to refresh configuration. Using the last saved settings.")
            }
            StartupState(StartupStatus.UNAVAILABLE, warning = "Unable to check application availability. Check your connection and retry.")
        }
    }
    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        try { api.health().useResponse() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { false }
    }
    private fun Response<okhttp3.ResponseBody>.useResponse(): Boolean {
        body()?.close(); errorBody()?.close(); return isSuccessful
    }
}
