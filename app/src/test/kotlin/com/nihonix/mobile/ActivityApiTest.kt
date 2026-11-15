package com.nihonix.mobile
import com.google.gson.JsonParser
import com.nihonix.mobile.core.network.interceptor.*
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.activity.*
import com.nihonix.mobile.feature.notifications.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class ActivityApiTest{
 private val server=MockWebServer()
 private val users=object:UserStorage{
  override val currentUserFlow=MutableStateFlow<User?>(User("a","learner","a@example.test"))
  override fun getUser()=currentUserFlow.value
  override fun saveUser(user:User){currentUserFlow.value=user}
  override fun clearUser(){currentUserFlow.value=null}
 }
 private val tokens=object:TokenStorage{
  override val isAuthenticated=MutableStateFlow(true)
  override fun getAccessToken()="access";override fun getRefreshToken()="refresh";override fun hasAccessToken()=true
  override fun saveTokens(accessToken:String,refreshToken:String){};override fun clearTokens(){}
 }
 private lateinit var activity:ActivityApi;private lateinit var notifications:NotificationsApi
 @Before fun setup(){
  server.start();val retrofit=Retrofit.Builder().baseUrl(server.url("/api/"))
   .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens,users)).build()).addConverterFactory(GsonConverterFactory.create()).build()
  activity=retrofit.create(ActivityApi::class.java);notifications=retrofit.create(NotificationsApi::class.java)
 }
 @After fun close(){server.shutdown()}
 @Test fun activityRetryPreservesUuidTimestampTimezoneAndDuration():Unit=runBlocking{
  repeat(2){server.enqueue(MockResponse().setBody("""{"success":true,"data":{"accepted":["id"]}}"""))}
  val body=ActivityBatch(listOf(ActivitySegment("id","grammar","2026-11-15T16:59:30Z",60,"Asia/Ho_Chi_Minh")))
  activity.batch(body,SessionRequest("a"));activity.batch(body,SessionRequest("a"))
  val first=server.takeRequest();val payload=first.body.readUtf8()
  assertEquals("/api/learning-streaks/activities/batch",first.path);assertEquals("a",first.getHeader("X-Nihonix-Account"))
  assertEquals(payload,server.takeRequest().body.readUtf8())
  val event=JsonParser.parseString(payload).asJsonObject["events"].asJsonArray.single().asJsonObject
  assertEquals(60,event["duration_seconds"].asInt);assertEquals("Asia/Ho_Chi_Minh",event["timezone"].asString)
 }
 @Test fun deviceBindingUsesJwtIdentityAndAnInstallationPath():Unit=runBlocking{
  server.enqueue(MockResponse().setBody("""{"success":true,"data":{"installation_id":"install","binding_id":"binding","enabled":true}}"""))
  assertEquals("binding",notifications.register("install",DeviceRegistration("token",true),SessionRequest("a")).body()!!.data!!.binding_id)
  val request=server.takeRequest();assertEquals("PUT",request.method);assertEquals("/api/devices/install",request.path)
  val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
  assertEquals(setOf("token","enabled"),body.keySet());assertEquals("Bearer access",request.getHeader("Authorization"))
 }
 @Test fun sessionPinnedRequestsCannotAdoptAnotherLoginForTheSameOwner():Unit=runBlocking{
  server.enqueue(MockResponse().setBody("""{"success":true,"data":{"installation_id":"install","binding_id":"binding","enabled":true}}"""))
  try{notifications.register("install",DeviceRegistration("token",true),SessionRequest("a","old-session"));fail("Stale session must not reach network")}catch(_:java.io.IOException){}
  assertEquals(0,server.requestCount)
 }
 @Test fun accountSwitchStopsDeviceRegistrationBeforeCredentialsAreSent():Unit=runBlocking{
  users.saveUser(User("b","other","b@example.test"))
  try{notifications.register("install",DeviceRegistration("token",true),SessionRequest("a"));fail("Stale account must not reach network")}catch(_:java.io.IOException){}
  assertEquals(0,server.requestCount)
 }
}
