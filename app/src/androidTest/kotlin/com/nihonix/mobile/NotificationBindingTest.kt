package com.nihonix.mobile
import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.OfflineDatabase
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.activity.*
import com.nihonix.mobile.feature.notifications.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import retrofit2.Response
@RunWith(AndroidJUnit4::class)
class NotificationBindingTest{
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private lateinit var db:OfflineDatabase;private lateinit var manager:NotificationsManager
 private val users=FlashUsers();private val tokens=FlashTokens()
 private val data=mapOf("schema_version" to "1","type" to "learning_reminder","destination" to "activity","user_id" to "account-a","binding_id" to "binding","notification_id" to "id")
 @Before fun open(){
  context.deleteDatabase("sprint08-binding.db");db=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint08-binding.db").build()
  val api=object:NotificationsApi{
   override suspend fun register(id:String,body:DeviceRegistration,session:SessionRequest)=Response.success(ApiResponseDto(success=true,data=DeviceBinding(id,"new-binding",true)))
   override suspend fun remove(id:String,session:SessionRequest):Response<Unit> = Response.success(Unit)
  }
  manager=NotificationsManager(context,"test",api,users,tokens,ActivityRepository(db,ActivityTestApi(),users,tokens,Gson(),{}))
  context.getSharedPreferences("nihonix_push_state",Context.MODE_PRIVATE).edit()
   .putString("test:owner","account-a").putString("test:binding","binding").remove("test:session")
   .putBoolean("test:enabled",true).putBoolean("test:reminder_enabled",true).commit()
 }
 @After fun close(){db.close();context.deleteDatabase("sprint08-binding.db");manager.disableLocal()}
 @Test fun coldStartupPreservesAMatchingDurableBinding(){
  assertNotNull(manager.accepted(data));manager.accountObserved();assertNotNull(manager.accepted(data))
 }
 @Test fun disablingReminderSuppressesAlreadyQueuedPayload(){
  manager.preferencesChanged("account-a",ActivityPreferences(push_enabled=true,reminder_enabled=false))
  assertNull(manager.accepted(data))
 }
}
