package com.nihonix.mobile

import com.google.gson.JsonParser
import com.nihonix.mobile.core.network.interceptor.*
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.progress.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProgressApiTest {
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
    private lateinit var api:ProgressApi
    @Before fun setup() {
        server.start()
        api=Retrofit.Builder().baseUrl(server.url("/api/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens,users)).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(ProgressApi::class.java)
    }
    @After fun close(){server.shutdown()}
    @Test fun batchPinsOwnerAndReusesImmutableWireMutation()=runBlocking {
        val id="91f74bd0-65e1-4f9c-b984-30fd5a84dbe3"
        val mutation=ProgressMutation(id,"vocabulary","hello","learned","2026-11-15T00:00:00Z")
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"acknowledgements":[{"mutation_id":"$id","status":"accepted"}]}}"""))
        val response=api.batch(ProgressBatch(listOf(mutation)),SessionRequest("a"))
        assertEquals(id,response.body()!!.data!!.getAsJsonArray("acknowledgements")[0].asJsonObject["mutation_id"].asString)
        val request=server.takeRequest()
        assertEquals("/api/progress/batch",request.path);assertEquals("POST",request.method)
        assertEquals("a",request.getHeader("X-Nihonix-Account"));assertEquals("Bearer access",request.getHeader("Authorization"))
        val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertFalse(body.has("user_id"));assertEquals(id,body.getAsJsonArray("mutations")[0].asJsonObject["mutation_id"].asString)
    }
    @Test fun deltaKeepsDecimalCursorAndOwnerPin()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"events":[],"cursor":"9007199254740993","has_more":false}}"""))
        val response=api.changes("9007199254740992",SessionRequest("a"),20)
        assertEquals("9007199254740993",response.body()!!.data!!["cursor"].asString)
        val request=server.takeRequest()
        assertEquals("9007199254740992",request.requestUrl!!.queryParameter("after"))
        assertEquals("20",request.requestUrl!!.queryParameter("limit"));assertEquals("a",request.getHeader("X-Nihonix-Account"))
    }
    @Test fun changedAccountRejectsRequestBeforeSendingCredentials():Unit=runBlocking {
        users.saveUser(User("b","other","b@example.test"))
        try {
            api.batch(ProgressBatch(listOf(ProgressMutation("id","vocabulary","hello","learned","time"))),SessionRequest("a"))
            fail("Changed account must stop before network")
        } catch(_:java.io.IOException) {}
        assertEquals(0,server.requestCount)
    }
}
