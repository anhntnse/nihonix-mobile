package com.nihonix.mobile.feature.system

import com.google.gson.annotations.SerializedName

data class AppConfig(
    @SerializedName("schema_version") val schemaVersion: Int? = null,
    @SerializedName("maintenance") val maintenance: Boolean? = null,
    @SerializedName("maintenance_message") val maintenanceMessage: String? = null,
    @SerializedName("minimum_version_code") val minimumVersionCode: Long? = null,
    @SerializedName("update_url") val updateUrl: String? = null,
    @SerializedName("cache_ttl_seconds") val cacheTtlSeconds: Long? = null
) {
    fun validated(): AppConfig {
        require(schemaVersion == 1 && maintenance != null)
        require(minimumVersionCode != null && minimumVersionCode >= 1)
        require(cacheTtlSeconds != null && cacheTtlSeconds in 60..86400)
        val uri = java.net.URI(updateUrl ?: "")
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null)
        require((maintenanceMessage?.length ?: 0) <= 500)
        return this
    }
    fun restriction(version: Long): StartupStatus = when {
        maintenance == true -> StartupStatus.MAINTENANCE
        version < (minimumVersionCode ?: Long.MAX_VALUE) -> StartupStatus.UPDATE_REQUIRED
        else -> StartupStatus.READY
    }
}
enum class StartupStatus { LOADING, READY, UNAVAILABLE, MAINTENANCE, UPDATE_REQUIRED }
data class StartupState(
    val status: StartupStatus = StartupStatus.LOADING,
    val config: AppConfig? = null,
    val warning: String? = null
)
