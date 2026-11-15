package com.nihonix.mobile

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nihonix.mobile.core.network.interceptor.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.auth.data.api.AuthApiService
import com.nihonix.mobile.feature.auth.data.repository.AuthRepositoryImpl
import com.nihonix.mobile.feature.auth.data.api.AccountApiService
import com.nihonix.mobile.feature.auth.data.repository.AccountRepositoryImpl
import com.nihonix.mobile.feature.auth.domain.model.LearningSettings
import com.nihonix.mobile.feature.auth.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AccountRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var users: MemoryUsers
    private lateinit var tokens: MemoryTokens
    private lateinit var repo: AccountRepositoryImpl
    private lateinit var auth: AuthRepositoryImpl

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        users = MemoryUsers()
        tokens = MemoryTokens()
        val retrofit = Retrofit.Builder().baseUrl(server.url("/api/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens, users)).build())
            .addConverterFactory(GsonConverterFactory.create()).build()
        repo = AccountRepositoryImpl(retrofit.create(AccountApiService::class.java), tokens, users, Gson())
        auth = AuthRepositoryImpl(retrofit.create(AuthApiService::class.java), tokens, users, Gson())
    }
    @After fun close() { server.shutdown() }

    @Test fun settingsUseTheAgreedSnakeCaseContract() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"push_enabled":true,"reminder_enabled":true,"reminder_time":"21:30","timezone":"Asia/Ho_Chi_Minh","daily_goal_minutes":30}}"""))
        val result = repo.updateSettings(LearningSettings(true, true, "21:30", "Asia/Ho_Chi_Minh", 30)).toList().last()
        assertTrue(result is Resource.Success)
        assertEquals(30, (result as Resource.Success).data.dailyGoalMinutes)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/users/me/settings", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(30, body["daily_goal_minutes"].asInt)
        assertFalse(body.has("dailyGoalMinutes"))
    }

    @Test fun verificationRequestsNeverAttachTheSignedInUsersCredentials() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"retry_after_seconds":120}}"""))
        val result = repo.requestVerification(" learner@example.test ").toList().last()
        assertEquals(120, (result as Resource.Success).data)
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertEquals("/api/auth/request-email-verification", request.path)
    }

    @Test fun missingVerificationAccountPreservesThe404Message() = runTest {
        val message = "Không tìm thấy tài khoản nào đăng ký với email này."
        server.enqueue(MockResponse().setResponseCode(404).setBody(
            """{"success":false,"message":"Không tìm thấy tài khoản nào đăng ký với email này.","data":null,"statusCode":404}"""
        ))
        val result = repo.requestVerification("unknown@example.test").toList().last() as Resource.Error
        assertEquals(404, result.statusCode)
        assertEquals(message, result.message)
        assertEquals("/api/auth/request-email-verification", server.takeRequest().path)
    }

    @Test fun missingSettingsPayloadIsEmptyInsteadOfAnUnsafeUnitCast() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}"""))
        assertEquals(Resource.Empty, repo.getSettings().toList().last())
    }

    @Test fun delete204ClearsTheLocalSessionOnlyAfterServerSuccess() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(repo.deleteAccount().toList().last() is Resource.Success)
        assertFalse(tokens.hasAccessToken())
        assertNull(users.getUser())
        assertEquals("/api/users/u1", server.takeRequest().path)
    }

    @Test fun successfulDeletionInvalidatesSessionBeforePurgingOfflineData() = runTest {
        val api=Retrofit.Builder().baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(AccountApiService::class.java)
        var purged:String?=null
        val deleting=AccountRepositoryImpl(api,tokens,users,Gson()) { owner ->
            assertFalse("Late sync must fail its session check before purge begins",tokens.hasAccessToken())
            assertNull(users.getUser())
            purged=owner
        }
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(deleting.deleteAccount().toList().last() is Resource.Success)
        assertEquals("u1",purged)
    }
    @Test fun deletionFailurePreservesTheLocalAccount() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(repo.deleteAccount().toList().last() is Resource.Error)
        assertTrue(tokens.hasAccessToken())
        assertNotNull(users.getUser())
    }

    @Test fun aProfileForAnotherAccountCannotOverwriteTheCachedUser() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"id":"someone-else","username":"other","email":"other@example.test"}}"""))
        assertTrue(repo.updateProfile("learner", "Name").toList().last() is Resource.Error)
        assertEquals("u1", users.getUser()?.id)
    }

    @Test fun unsupportedAvatarInputNeverReachesTheNetwork() = runTest {
        assertTrue(repo.uploadAvatar(byteArrayOf(1, 2, 3), "image/svg+xml").toList().last() is Resource.Error)
        assertEquals(0, server.requestCount)
    }

    @Test fun profileFetchRestoresAMissingCacheForTheSameSession() = runTest {
        users.clearUser()
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"id":"u1","username":"learner","email":"learner@example.test"}}"""))
        assertTrue(auth.getCurrentUser().toList().last() is Resource.Success)
        assertEquals("u1", users.getUser()?.id)
    }

    @Test fun anOlderProfileFetchCannotOverwriteAnEditThatCompletedInFlight() = runTest {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                users.saveUser(users.getUser()!!.copy(username = "updated"))
                return MockResponse().setBody("""{"success":true,"data":{"id":"u1","username":"old","email":"learner@example.test"}}""")
            }
        }
        val result = auth.getCurrentUser().toList().last() as Resource.Success
        assertEquals("updated", result.data.username)
        assertEquals("updated", users.getUser()?.username)
    }

    @Test fun aLateProfileResponseCannotOverwriteAnotherSignedInAccount() = runTest {
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                users.saveUser(User("u2", "other", "other@example.test"))
                return MockResponse().setBody("""{"success":true,"data":{"id":"u1","username":"old","email":"learner@example.test"}}""")
            }
        }
        assertTrue(auth.getCurrentUser().toList().last() is Resource.Error)
        assertEquals("u2", users.getUser()?.id)
    }

    private class MemoryTokens : TokenStorage {
        private var access: String? = "access"
        private var refresh: String? = "refresh"
        override val isAuthenticated = MutableStateFlow(true)
        override fun getAccessToken() = access
        override fun getRefreshToken() = refresh
        override fun hasAccessToken() = access != null
        override fun saveTokens(accessToken: String, refreshToken: String) { access = accessToken; refresh = refreshToken; isAuthenticated.value = true }
        override fun clearTokens() { access = null; refresh = null; isAuthenticated.value = false }
    }
    private class MemoryUsers : UserStorage {
        private val user = MutableStateFlow<User?>(User("u1", "learner", "learner@example.test"))
        override val currentUserFlow: StateFlow<User?> = user
        override fun getUser() = user.value
        override fun saveUser(user: User) { this.user.value = user }
        override fun clearUser() { user.value = null }
    }
}
