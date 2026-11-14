package com.nihonix.mobile.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TokenStorage {
    val isAuthenticated: StateFlow<Boolean>
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
    fun hasAccessToken(): Boolean
}

class SecureStorageUnavailableException : IllegalStateException(
    "Secure sign-in storage is unavailable. Restart the app and try again."
)

/** Never reads or writes credentials through an unencrypted fallback. */
class SecureTokenStorage internal constructor(
    createEncryptedPreferences: () -> SharedPreferences
) : TokenStorage {
    constructor(context: Context) : this({
        // Discard credentials written by older versions' insecure fallback.
        context.deleteSharedPreferences("nihonix_tokens_fallback")
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    })

    private val prefs = runCatching(createEncryptedPreferences).getOrNull()
    private val _isAuthenticated = MutableStateFlow(
        !readToken(KEY_ACCESS_TOKEN).isNullOrBlank() && !readToken(KEY_REFRESH_TOKEN).isNullOrBlank()
    )
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    override fun saveTokens(accessToken: String, refreshToken: String) {
        require(accessToken.isNotBlank() && refreshToken.isNotBlank()) { "Invalid authentication tokens." }
        val encryptedPrefs = prefs ?: throw SecureStorageUnavailableException()
        val saved = runCatching {
            encryptedPrefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .commit()
        }.getOrDefault(false)
        if (!saved) {
            clearTokens()
            throw SecureStorageUnavailableException()
        }
        _isAuthenticated.value = true
    }

    override fun getAccessToken(): String? =
        if (_isAuthenticated.value) readToken(KEY_ACCESS_TOKEN) else null

    override fun getRefreshToken(): String? =
        if (_isAuthenticated.value) readToken(KEY_REFRESH_TOKEN) else null

    override fun clearTokens() {
        _isAuthenticated.value = false
        runCatching { prefs?.edit()?.clear()?.apply() }
    }

    override fun hasAccessToken(): Boolean = !getAccessToken().isNullOrBlank()

    private fun readToken(key: String): String? =
        runCatching { prefs?.getString(key, null) }.getOrNull()

    companion object {
        private const val PREFS_NAME = "nihonix_secure_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
