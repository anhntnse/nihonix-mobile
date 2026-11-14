package com.nihonix.mobile

import android.content.SharedPreferences
import com.google.gson.Gson
import com.nihonix.mobile.core.network.ApiEndpoint
import com.nihonix.mobile.core.network.NetworkModule
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.SecureStorageUnavailableException
import com.nihonix.mobile.core.storage.SecureTokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.auth.data.api.AuthApiService
import com.nihonix.mobile.feature.auth.data.dto.LoginResponseDto
import com.nihonix.mobile.feature.auth.data.dto.UserDto
import com.nihonix.mobile.feature.auth.data.repository.AuthRepositoryImpl
import com.nihonix.mobile.feature.auth.domain.model.User
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class SecurityFoundationTest {
    @Test
    fun releaseEndpointRejectsHttpLocalCredentialsAndAmbiguousUrls() {
        val invalid = listOf(
            "", "http://api.example.test/api/", "https://localhost/api/",
            "https://user:password@api.example.test/api/",
            "https://api.example.test/api/?token=secret",
            "https://api.example.test/api/#fragment",
            "https://api.example.test/api"
        )
        invalid.forEach { url ->
            assertThrows(IllegalArgumentException::class.java) {
                ApiEndpoint.validate(url, allowDevelopmentHttp = false)
            }
        }
        ApiEndpoint.validate("https://api.example.test/api/", allowDevelopmentHttp = false)
    }

    @Test
    fun debugHttpIsRestrictedToTheLocalHostsAllowedByNetworkSecurityConfig() {
        listOf("10.0.2.2", "localhost", "127.0.0.1").forEach { host ->
            ApiEndpoint.validate("http://$host:3010/api/", allowDevelopmentHttp = true)
            assertTrue(ApiEndpoint.isLocalDevelopment("http://$host:3010/api/"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ApiEndpoint.validate("http://api.example.test/api/", allowDevelopmentHttp = true)
        }
        assertFalse(ApiEndpoint.isLocalDevelopment("https://api.example.test/api/"))
    }

    @Test
    fun encryptedStorageInitializationFailureLeavesNoSessionAndRejectsSaving() {
        val storage = SecureTokenStorage { throw IllegalStateException("Keystore unavailable") }
        assertFalse(storage.isAuthenticated.value)
        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
        assertThrows(SecureStorageUnavailableException::class.java) {
            storage.saveTokens("access", "refresh")
        }
        storage.clearTokens()
        assertFalse(storage.hasAccessToken())
    }

    @Test
    fun tokensRotatePersistAndLogoutInvalidatesTheObservableSession() {
        val preferences = MemoryPreferences()
        val storage = SecureTokenStorage { preferences.value }
        assertFalse(storage.isAuthenticated.value)
        storage.saveTokens("access-1", "refresh-1")
        assertTrue(storage.isAuthenticated.value)
        storage.saveTokens("access-2", "refresh-2")
        val restored = SecureTokenStorage { preferences.value }
        assertEquals("access-2", restored.getAccessToken())
        assertEquals("refresh-2", restored.getRefreshToken())
        storage.clearTokens()
        assertFalse(storage.isAuthenticated.value)
        assertNull(storage.getAccessToken())
        assertFalse(SecureTokenStorage { preferences.value }.hasAccessToken())
    }

    @Test
    fun failedPersistenceDoesNotEstablishASession() {
        val preferences = MemoryPreferences(commitSucceeds = false)
        val storage = SecureTokenStorage { preferences.value }
        assertThrows(SecureStorageUnavailableException::class.java) {
            storage.saveTokens("access", "refresh")
        }
        assertFalse(storage.isAuthenticated.value)
        assertNull(storage.getAccessToken())
    }

    @Test
    fun incompletePersistedTokenPairDoesNotRestoreASession() {
        val preferences = MemoryPreferences()
        preferences.values["access_token"] = "orphaned-access"
        val storage = SecureTokenStorage { preferences.value }
        assertFalse(storage.isAuthenticated.value)
        assertNull(storage.getAccessToken())
    }

    @Test
    fun networkClientHasNoRequestBodyOrCredentialLoggingInterceptor() {
        val storage = SecureTokenStorage { MemoryPreferences().value }
        val client = NetworkModule(
            storage, MemoryUserStorage(), Gson(), "https://api.example.test/api/"
        ).okHttpClient
        assertEquals(1, client.interceptors.size)
        assertEquals("AuthInterceptor", client.interceptors.single().javaClass.simpleName)
    }

    @Test
    fun loginReportsSecureStorageFailureInsteadOfSuccessOrAnUncaughtFlowError() = runTest {
        val storage = SecureTokenStorage { throw IllegalStateException("Keystore unavailable") }
        val userStorage = MemoryUserStorage()
        val login = LoginResponseDto(
            UserDto("user-1", "learner", "learner@example.test"), "access", "refresh"
        )
        val api = Proxy.newProxyInstance(
            AuthApiService::class.java.classLoader, arrayOf(AuthApiService::class.java)
        ) { _, method, _ ->
            check(method.name == "login")
            Response.success(ApiResponseDto(success = true, data = login))
        } as AuthApiService
        val repository = AuthRepositoryImpl(api, storage, userStorage, Gson())
        val results = repository.login("learner", "password").toList()
        assertTrue(results.first() is Resource.Loading)
        assertTrue(results.last() is Resource.Error)
        assertFalse(results.any { it is Resource.Success })
        assertFalse(storage.hasAccessToken())
        assertNull(userStorage.getUser())
    }

    private class MemoryUserStorage : UserStorage {
        private val user = MutableStateFlow<User?>(null)
        override val currentUserFlow: StateFlow<User?> = user
        override fun saveUser(user: User) { this.user.value = user }
        override fun getUser(): User? = user.value
        override fun clearUser() { user.value = null }
    }

    /** Android interfaces backed by JVM memory; no Keystore or framework stubs executed. */
    private class MemoryPreferences(private val commitSucceeds: Boolean = true) {
        val values = mutableMapOf<String, String?>()
        val value: SharedPreferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getString" -> values[args!![0] as String] ?: args[1]
                "edit" -> editor()
                else -> error("Unexpected preference operation: ${method.name}")
            }
        } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, String?>()
            var clear = false
            fun applyChanges() {
                if (clear) values.clear()
                pending.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
            }
            return Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java)
            ) { proxy, method, args ->
                when (method.name) {
                    "putString" -> { pending[args!![0] as String] = args[1] as String?; proxy }
                    "clear" -> { clear = true; proxy }
                    "commit" -> { if (commitSucceeds) applyChanges(); commitSucceeds }
                    "apply" -> { applyChanges(); null }
                    else -> error("Unexpected editor operation: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
    }
}
