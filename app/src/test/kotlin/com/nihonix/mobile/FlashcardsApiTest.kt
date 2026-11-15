package com.nihonix.mobile

import com.google.gson.JsonParser
import com.nihonix.mobile.core.network.interceptor.*
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.flashcards.data.*
import com.nihonix.mobile.feature.flashcards.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FlashcardsApiTest {
    private val server=MockWebServer()
    private val users=object:UserStorage {
        override val currentUserFlow=MutableStateFlow<User?>(User("a","learner","a@example.test"))
        override fun getUser()=currentUserFlow.value
        override fun saveUser(user:User){currentUserFlow.value=user}
        override fun clearUser(){currentUserFlow.value=null}
    }
    private val tokens=object:TokenStorage {
        override val isAuthenticated=MutableStateFlow(true)
        override fun getAccessToken()="access"
        override fun getRefreshToken()="refresh"
        override fun hasAccessToken()=true
        override fun saveTokens(accessToken:String,refreshToken:String){}
        override fun clearTokens(){}
    }
    private lateinit var api:FlashcardsApi
    @Before fun setup() {
        server.start()
        api=Retrofit.Builder().baseUrl(server.url("/api/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens,users)).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(FlashcardsApi::class.java)
    }
    @After fun close(){server.shutdown()}
    @Test fun batchPreservesFullAggregateDependencyAndPinnedOwner()=runBlocking {
        val mutation=FlashMutation("mutation","set","set-id","upsert","9007199254740993",depends_on="prior",
            name="Japanese",folder_id="folder-id",cards=listOf(CardDraft("card-id",CardFace("猫"),CardFace("Cat"),1)))
        repeat(2){server.enqueue(MockResponse().setBody("""{"success":true,"data":{"acknowledgements":[{"mutation_id":"mutation","kind":"set","id":"set-id","version":"9007199254740994","status":"accepted","deleted_ids":{"folders":[],"sets":[],"cards":["removed"]}}]}}"""))}
        val response=api.batch(FlashBatch(listOf(mutation)),SessionRequest("a"))
        api.batch(FlashBatch(listOf(mutation)),SessionRequest("a"))
        val ack=response.body()!!.data!!.acknowledgements.single()
        assertEquals("9007199254740994",ack.version);assertEquals(listOf("removed"),ack.deleted_ids!!.cards)
        val request=server.takeRequest()
        val json=request.body.readUtf8()
        assertEquals(json,server.takeRequest().body.readUtf8())
        assertEquals("/api/flashcards/sync/batch",request.path);assertEquals("POST",request.method)
        assertEquals("Bearer access",request.getHeader("Authorization"));assertEquals("a",request.getHeader("X-Nihonix-Account"))
        val body=JsonParser.parseString(json).asJsonObject
        assertFalse(body.has("user_id"))
        val wire=body.getAsJsonArray("mutations").single().asJsonObject
        assertEquals("9007199254740993",wire["base_version"].asString);assertEquals("prior",wire["depends_on"].asString)
        assertEquals("folder-id",wire["folder_id"].asString)
        assertEquals("猫",wire.getAsJsonArray("cards").single().asJsonObject.getAsJsonObject("front")["text"].asString)
    }
    @Test fun deltaPreservesCursorTombstoneAndHasMore()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"events":[{"mutation_id":"delete","kind":"set","id":"set-id","version":"9007199254740994","status":"accepted","record":{"kind":"set","id":"set-id","user_id":"a","version":"9007199254740994","deleted":true,"folder_id":"folder-id"},"deleted_ids":{"folders":[],"sets":["set-id"],"cards":["card-id"]}}],"cursor":"9007199254740994","has_more":true}}"""))
        val delta=api.changes("9007199254740993",SessionRequest("a"),20).body()!!.data!!
        assertEquals("9007199254740994",delta.cursor);assertTrue(delta.has_more)
        assertTrue(delta.events.single().record!!.deleted)
        val request=server.takeRequest()
        assertEquals("/api/flashcards/sync/changes",request.requestUrl!!.encodedPath)
        assertEquals("9007199254740993",request.requestUrl!!.queryParameter("after"))
        assertEquals("20",request.requestUrl!!.queryParameter("limit"));assertEquals("a",request.getHeader("X-Nihonix-Account"))
    }
    @Test fun changedAccountStopsPrivateRequestBeforeCredentialsAreSent():Unit=runBlocking {
        users.saveUser(User("b","other","b@example.test"))
        try {api.changes("0",SessionRequest("a"));fail("Changed account must stop before network")}
        catch(_:java.io.IOException){}
        assertEquals(0,server.requestCount)
    }
}
