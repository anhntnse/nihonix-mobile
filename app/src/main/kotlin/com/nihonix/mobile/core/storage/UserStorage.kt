package com.nihonix.mobile.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.nihonix.mobile.feature.auth.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserStorage {
    val currentUserFlow: StateFlow<User?>
    fun saveUser(user: User)
    fun getUser(): User?
    fun clearUser()
}

class SharedPreferencesUserStorage(
    context: Context,
    private val gson: Gson
) : UserStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _currentUserFlow = MutableStateFlow<User?>(getUser())
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override fun saveUser(user: User) {
        val json = gson.toJson(user)
        prefs.edit().putString(KEY_USER, json).apply()
        _currentUserFlow.value = user
    }

    override fun getUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun clearUser() {
        prefs.edit().remove(KEY_USER).apply()
        _currentUserFlow.value = null
    }

    companion object {
        private const val PREFS_NAME = "nihonix_user_storage"
        private const val KEY_USER = "current_user"
    }
}
