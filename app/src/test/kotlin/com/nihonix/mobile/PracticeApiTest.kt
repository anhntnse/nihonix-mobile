package com.nihonix.mobile

import com.google.gson.JsonParser
import com.nihonix.mobile.core.network.interceptor.*
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.practice.data.*
import com.nihonix.mobile.feature.practice.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PracticeApiTest {
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
    private lateinit var api:PracticeApi
    @Before fun setup() {
        server.start()
        api=Retrofit.Builder().baseUrl(server.url("/api/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens,users)).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(PracticeApi::class.java)
    }
    @After fun close(){server.shutdown()}

    @Test fun quotaIncludesReservationsResetAndAccountPin()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"day":"2026-11-15","resets_at":"2026-11-15T17:00:00.000Z","timezone":"Asia/Ho_Chi_Minh","max_items":15,"provider":{"configured":true},"buckets":{"practice":{"limit":3,"used":1,"reserved":1,"remaining":1},"flashcards":{"limit":2,"used":0,"reserved":0,"remaining":2}}}}"""))
        val quota=api.quota(SessionRequest("a")).body()!!.data!!
        assertEquals(1,quota.buckets.getValue("practice").reserved);assertEquals("2026-11-15T17:00:00.000Z",quota.resets_at)
        val request=server.takeRequest()
        assertEquals("/api/ai/quota",request.path);assertEquals("a",request.getHeader("X-Nihonix-Account"))
        assertEquals("Bearer access",request.getHeader("Authorization"))
    }
    @Test fun generationRetriesKeepTheOriginalUuidAndPayload()=runBlocking {
        repeat(2){server.enqueue(MockResponse().setBody("""{"success":true,"data":{"request_id":"request","kind":"fill_blank","status":"reserved","result":null,"error":null}}"""))}
        val input=GeneratePractice("request","topic",3)
        assertEquals("reserved",api.fill(input,SessionRequest("a")).body()!!.data!!.status)
        api.fill(input,SessionRequest("a"))
        val first=server.takeRequest()
        val json=first.body.readUtf8()
        assertEquals(json,server.takeRequest().body.readUtf8())
        assertEquals("/api/practice/fill-blank",first.path)
        val body=JsonParser.parseString(json).asJsonObject
        assertEquals("request",body["request_id"].asString);assertEquals(3,body["exercise_count"].asInt)
        assertFalse(body.has("user_id"))
    }
    @Test fun submissionKeepsNumericChoicesAndReadsCanonicalCompletion()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"request_id":"request","session_id":"session","total_exercises":1,"correct_count":1,"accuracy":100,"total_time":5,"completion_id":"completion","completed_at":"2026-11-15T00:00:00Z","results":[{"exercise_id":"question","user_answer":1,"is_correct":true,"correct_answer":1,"explanation":{"vi":"Mèo","en":"Cat"},"time_spent":5}]}}"""))
        val input=SubmitPractice("request","session",listOf(PracticeAnswer("question",com.google.gson.JsonPrimitive(1),5)))
        val result=api.submit(input,SessionRequest("a")).body()!!.data!!
        assertEquals("completion",result.completion_id);assertEquals(1,result.results.single().correct_answer.asInt)
        val request=server.takeRequest()
        assertEquals("/api/practice/submit",request.path);assertEquals("a",request.getHeader("X-Nihonix-Account"))
        val answer=JsonParser.parseString(request.body.readUtf8()).asJsonObject.getAsJsonArray("answers").single().asJsonObject
        assertTrue(answer["user_answer"].asJsonPrimitive.isNumber)
    }
    @Test fun changedAccountStopsGenerationBeforeSendingCredentials():Unit=runBlocking {
        users.saveUser(User("b","other","b@example.test"))
        try {api.fill(GeneratePractice("request","topic",1),SessionRequest("a"));fail("Changed account must stop before network")}
        catch(_:java.io.IOException){}
        assertEquals(0,server.requestCount)
    }
}
