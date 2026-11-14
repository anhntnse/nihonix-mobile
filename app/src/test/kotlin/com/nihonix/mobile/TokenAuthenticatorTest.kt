package com.nihonix.mobile

import com.google.gson.Gson
import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.authenticator.TokenAuthenticator
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.auth.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: TestTokens
    private lateinit var users: TestUsers
    private lateinit var authenticator: TokenAuthenticator

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        tokens = TestTokens()
        users = TestUsers()
        authenticator = TokenAuthenticator(tokens, users, Gson(), server.url("/api/").toString())
    }
    @After fun close() { server.shutdown() }

    private fun unauthorized(authenticated: Boolean = true, tag: SessionRequest? = null): Response {
        val request = Request.Builder().url(server.url("/api/users/me/settings"))
        if (authenticated) request.header(ApiConstants.HEADER_AUTHORIZATION, "Bearer old-access")
        if (tag != null) request.tag(SessionRequest::class.java, tag)
        return Response.Builder().request(request.build()).protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
    }

    @Test fun rotatesTokensAndRetriesTheOriginalRequest() {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"accessToken":"new-access","refreshToken":"new-refresh"}}"""))
        val request = authenticator.authenticate(null, unauthorized())
        assertEquals("Bearer new-access", request?.header("Authorization"))
        assertEquals("new-refresh", tokens.getRefreshToken())
        assertEquals("/api/auth/refresh", server.takeRequest().path)
    }

    @Test fun temporaryRefreshFailureKeepsTheSessionForRetry() {
        server.enqueue(MockResponse().setResponseCode(503))
        assertNull(authenticator.authenticate(null, unauthorized()))
        assertTrue(tokens.hasAccessToken())
        assertNotNull(users.getUser())
    }

    @Test fun revokedRefreshClearsTokensAndCachedUser() {
        server.enqueue(MockResponse().setResponseCode(401))
        assertNull(authenticator.authenticate(null, unauthorized()))
        assertFalse(tokens.isAuthenticated.value)
        assertNull(users.getUser())
    }

    @Test fun public401NeverGetsCredentialsOrRefreshesTheSession() {
        assertNull(authenticator.authenticate(null, unauthorized(authenticated = false)))
        assertEquals(0, server.requestCount)
        assertTrue(tokens.hasAccessToken())
    }

    @Test fun staleFailureUsesTokensAlreadyRefreshedByAnotherRequest() {
        tokens.saveTokens("new-access", "new-refresh")
        val retry = authenticator.authenticate(null, unauthorized())
        assertEquals("Bearer new-access", retry?.header("Authorization"))
        assertEquals(0, server.requestCount)
    }

    @Test fun aRequestFromAnotherAccountIsNotRetriedWithTheCurrentAccountsCredentials() {
        assertNull(authenticator.authenticate(null, unauthorized(tag = SessionRequest("other-user"))))
        assertEquals(0, server.requestCount)
        assertTrue(tokens.hasAccessToken())
    }

    @Test fun retryLimitStopsARejectedAccessTokenLoop() {
        val original = unauthorized()
        val retried = original.newBuilder().priorResponse(original).build()
        assertNull(authenticator.authenticate(null, retried))
        assertFalse(tokens.hasAccessToken())
        assertEquals(0, server.requestCount)
    }

    private class TestTokens : TokenStorage {
        private var access: String? = "old-access"
        private var refresh: String? = "old-refresh"
        override val isAuthenticated = MutableStateFlow(true)
        override fun saveTokens(accessToken: String, refreshToken: String) {
            access = accessToken; refresh = refreshToken; isAuthenticated.value = true
        }
        override fun getAccessToken() = access
        override fun getRefreshToken() = refresh
        override fun hasAccessToken() = access != null
        override fun clearTokens() { access = null; refresh = null; isAuthenticated.value = false }
    }

    private class TestUsers : UserStorage {
        private val value = MutableStateFlow<User?>(User("user-1", "learner", "learner@example.test"))
        override val currentUserFlow: StateFlow<User?> = value
        override fun getUser() = value.value
        override fun saveUser(user: User) { value.value = user }
        override fun clearUser() { value.value = null }
    }
}
