package com.nihonix.mobile

import com.google.gson.Gson
import com.nihonix.mobile.core.network.NetworkModule
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.learning.data.LearningApi
import com.nihonix.mobile.feature.topics.data.api.TopicsLevelsApiService
import com.nihonix.mobile.feature.system.SystemApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException
import java.util.concurrent.*

class CatalogAuthorizationTest {
    private val server = MockWebServer()
    private val users = Users()
    private val tokens = Tokens()
    private val store = Store()
    private lateinit var network: NetworkModule
    private val payload = """{"success":true,"data":{"slug":"hello","kana":"こんにちは"}}"""
    private class Store : CatalogStore {
        val rows = linkedMapOf<String,CatalogCache>()
        override fun get(url:String) = rows[url]
        override fun put(row:CatalogCache) { rows[row.url] = row }
        override fun remove(url:String) { rows.remove(url) }
        override fun all() = rows.values.toList()
    }
    private class Users : UserStorage {
        override val currentUserFlow = MutableStateFlow<User?>(User("a","learner","a@example.test"))
        override fun getUser() = currentUserFlow.value
        override fun saveUser(user:User) { currentUserFlow.value = user }
        override fun clearUser() { currentUserFlow.value = null }
    }
    private class Tokens : TokenStorage {
        @Volatile var access:String? = jwt("session-a")
        private var refresh:String? = "refresh"
        override val isAuthenticated = MutableStateFlow(true)
        override fun getAccessToken() = access
        override fun getRefreshToken() = refresh
        override fun hasAccessToken() = access != null
        override fun clearTokens() { access = null; refresh = null; isAuthenticated.value = false }
        override fun saveTokens(accessToken:String,refreshToken:String) { access = accessToken; refresh = refreshToken; isAuthenticated.value = true }
    }
    companion object {
        private fun jwt(sid:String) = "e30." + java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"sid\":\"" + sid + "\"}").toByteArray()) + ".sig"
    }
    @Before fun setup() {
        server.start(java.net.InetAddress.getByName("127.0.0.1"),0)
        check(BuildConfig.DEBUG) { "Unit fixture requires debug BuildConfig" }
        check(com.nihonix.mobile.core.network.ApiEndpoint.isLocalDevelopment(url("/api/").toString())) { "Fixture URL: " + url("/api/") }
        network = NetworkModule(tokens,users,Gson(),url("/api/").toString(),store)
    }
    @After fun close() { server.shutdown() }
    private fun url(path:String) = server.url(path).newBuilder().host("127.0.0.1").build()
    private fun request() = Request.Builder().url(url("/api/catalog/vocabulary-items/hello")).build()

    @Test fun catalogApiMethodsSendTheSavedSessionButStartupRemainsPublic() = runBlocking {
        val topics = network.createService(TopicsLevelsApiService::class.java)
        val learning = network.createService(LearningApi::class.java)
        val calls:List<suspend ()->Unit> = listOf(
            { topics.getLevels(); Unit }, { topics.getLevelByCode("N5"); Unit },
            { topics.getTopics(); Unit }, { topics.getTopicDetail("hello"); Unit },
            { topics.getTopicBundle("hello"); Unit },
            { learning.page("vocabulary-items",null,null,null,null,null,1); Unit },
            { learning.detail("vocabulary-items","hello"); Unit }, { learning.topics(); Unit }
        )
        calls.forEach { call ->
            server.enqueue(MockResponse().setBody("""{"success":true,"data":null}"""))
            call()
            val sent = server.takeRequest()
            assertEquals("Bearer " + tokens.access,sent.getHeader("Authorization"))
            assertNull(sent.getHeader("No-Authentication"))
        }
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}"""))
        network.createService(SystemApi::class.java).config()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun authenticatedCatalogCachesPayloadAndWorksOfflineOnlyWhileLocallySignedIn() {
        server.enqueue(MockResponse().setBody(payload).setHeader("ETag","v1"))
        network.okHttpClient.newCall(request()).execute().use { assertEquals(payload,it.body!!.string()) }
        assertEquals(payload,store.get(request().url.toString())?.payload)
        server.enqueue(MockResponse().setResponseCode(503))
        network.okHttpClient.newCall(request()).execute().use {
            assertEquals(200,it.code); assertEquals("offline",it.header("X-Nihonix-Cache"))
        }
        tokens.clearTokens(); users.clearUser()
        server.enqueue(MockResponse().setResponseCode(503))
        network.okHttpClient.newCall(request()).execute().use {
            assertEquals(401,it.code); assertNull(it.header("X-Nihonix-Cache"))
        }
    }

    @Test fun loggedOutUserCannotReadAnExistingCacheEvenWhenServerIsUnavailable() {
        store.put(CatalogCache(request().url.toString(),payload,"v1",1))
        tokens.clearTokens(); users.clearUser()
        server.enqueue(MockResponse().setResponseCode(503))
        network.okHttpClient.newCall(request()).execute().use {
            assertEquals(401,it.code)
            assertFalse(it.body!!.string().contains("こんにちは"))
        }
    }

    @Test fun revokedSessionDoesNotFallBackToCachedContent() {
        store.put(CatalogCache(request().url.toString(),payload,"v1",1))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        network.okHttpClient.newCall(request()).execute().use {
            assertEquals(401,it.code); assertNull(it.header("X-Nihonix-Cache"))
        }
        assertFalse(tokens.hasAccessToken())
    }

    @Test fun delayedResponseFromPreviousLoginCannotPopulateCacheOrReachTheNewLogin() {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request:RecordedRequest):MockResponse {
                entered.countDown()
                check(release.await(5,TimeUnit.SECONDS))
                return MockResponse().setBody(payload)
            }
        }
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = executor.submit<Boolean> {
                try { network.okHttpClient.newCall(request()).execute().use { false } }
                catch (_:IOException) { true }
            }
            assertTrue(entered.await(5,TimeUnit.SECONDS))
            tokens.access = jwt("replacement-session") // same owner, different login
            release.countDown()
            assertTrue("Old login response reached the replacement login",result.get(5,TimeUnit.SECONDS))
            assertTrue(store.rows.isEmpty())
        } finally { release.countDown(); executor.shutdownNow() }
    }

    @Test fun refreshWithinTheSameSessionCanRestoreAConditionalPayload() {
        server.enqueue(MockResponse().setBody(payload).setHeader("ETag","v1"))
        network.okHttpClient.newCall(request()).execute().close()
        val refreshed=jwt("session-a").removeSuffix(".sig")+".new-signature"
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"accessToken":"$refreshed","refreshToken":"new-refresh"}}"""))
        server.enqueue(MockResponse().setResponseCode(304))
        network.okHttpClient.newCall(request()).execute().use {
            assertEquals(200,it.code); assertEquals(payload,it.body!!.string())
            assertEquals("validated",it.header("X-Nihonix-Cache"))
        }
        assertEquals(refreshed,tokens.access)
        assertEquals("/api/catalog/vocabulary-items/hello",server.takeRequest().path)
        assertEquals("v1",server.takeRequest().getHeader("If-None-Match"))
        assertEquals("/api/auth/refresh",server.takeRequest().path)
        assertEquals("Bearer "+refreshed,server.takeRequest().getHeader("Authorization"))
    }

    @Test fun noLoginDoesNotStartBackgroundCatalogRequests() {
        tokens.clearTokens();users.clearUser()
        CatalogSync(network.okHttpClient,store,url("/api/").toString(),network::currentCatalogSession).refresh()
        assertEquals(0,server.requestCount)
    }

    @Test fun backgroundRefreshNeverSendsCredentialsToAForeignCachedUrl() {
        val other=MockWebServer()
        other.start()
        try {
            store.put(CatalogCache(other.url("/api/topics").toString(),payload,null,1))
            server.enqueue(MockResponse().setBody("""{"success":true,"data":{"schema_version":1,"collections":[]}}"""))
            other.enqueue(MockResponse().setBody(payload))
            CatalogSync(network.okHttpClient,store,url("/api/").toString(),network::currentCatalogSession).refresh()
            assertEquals("Catalog sync followed an out-of-environment URL",0,other.requestCount)
        } finally {other.shutdown()}
    }

    @Test fun catalogSyncSendsTheCurrentSession() {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"schema_version":1,"collections":[]}}"""))
        CatalogSync(network.okHttpClient,store,url("/api/").toString(),network::currentCatalogSession).refresh()
        assertEquals("Bearer " + tokens.access,server.takeRequest().getHeader("Authorization"))
    }
}
