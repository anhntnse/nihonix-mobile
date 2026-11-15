package com.nihonix.mobile
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nihonix.mobile.core.network.interceptor.AuthInterceptor
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.auth.data.api.AuthApiService
import com.nihonix.mobile.feature.auth.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class AuthSessionSafetyTest{
 private fun jwt(sid:String)="e30."+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sid\":\""+sid+"\"}").toByteArray())+".sig"
 private val server=MockWebServer();private var access=jwt("session-a");private var refresh="old-refresh"
 private val users=object:UserStorage{
  override val currentUserFlow=MutableStateFlow<User?>(User("a","learner","a@example.test"))
  override fun getUser()=currentUserFlow.value;override fun saveUser(user:User){currentUserFlow.value=user};override fun clearUser(){currentUserFlow.value=null}
 }
 private val tokens=object:TokenStorage{
  override val isAuthenticated=MutableStateFlow(true)
  override fun getAccessToken()=access.takeIf{it.isNotBlank()};override fun getRefreshToken()=refresh
  override fun hasAccessToken()=access.isNotBlank()
  override fun saveTokens(accessToken:String,refreshToken:String){access=accessToken;refresh=refreshToken;isAuthenticated.value=true}
  override fun clearTokens(){access="";refresh="";isAuthenticated.value=false}
 }
 private lateinit var api:AuthApiService
 @Before fun setup(){server.start();api=Retrofit.Builder().baseUrl(server.url("/api/"))
  .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens,users)).build()).addConverterFactory(GsonConverterFactory.create()).build().create(AuthApiService::class.java)}
 @After fun close(){server.shutdown()}
 @Test fun logoutUsesTheRefreshTokenAfterDeviceCleanupRotation():Unit=runBlocking{
  server.enqueue(MockResponse().setBody("""{"success":true,"data":{"message":"Logged out"}}"""))
  val repo=AuthRepositoryImpl(api,tokens,users,Gson(),beforeLogout={tokens.saveTokens(jwt("session-a"),"rotated-refresh")})
  repo.logout().toList()
  val sent=JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
  assertEquals("rotated-refresh",sent["refreshToken"].asString)
 }
 @Test fun delayedLogoutDoesNotClearOrRevokeAReplacementSession():Unit=runBlocking{
  server.enqueue(MockResponse().setBody("""{"success":true,"data":{"message":"Logged out"}}"""))
  val repo=AuthRepositoryImpl(api,tokens,users,Gson(),beforeLogout={tokens.saveTokens(jwt("session-b"),"new-login-refresh")})
  repo.logout().toList()
  assertEquals("new-login-refresh",tokens.getRefreshToken());assertNotNull(users.getUser());assertEquals(0,server.requestCount)
 }
}
