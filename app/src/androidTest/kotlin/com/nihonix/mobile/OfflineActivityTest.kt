package com.nihonix.mobile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.feature.activity.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException
import java.util.UUID
internal class ActivityTestApi:ActivityApi{
 var currentStreak=1
 val receipts=mutableMapOf<String,ActivitySegment>();var lost=false;var duringReply:(()->Unit)?=null
 private fun <T> ok(value:T)=Response.success(ApiResponseDto(success=true,data=value))
 override suspend fun batch(body:ActivityBatch,session:SessionRequest):Response<ApiResponseDto<ActivityAck>>{
  body.events.forEach{receipts.putIfAbsent(session.userId+":"+it.id,it)}
  duringReply?.invoke();duringReply=null
  if(lost){lost=false;throw IOException("reply lost")}
  return ok(ActivityAck(body.events.map{it.id}))
 }
 override suspend fun preferences(session:SessionRequest)=ok(ActivityPreferences(timezone="Asia/Ho_Chi_Minh"))
 override suspend fun summary(session:SessionRequest)=ok(ActivitySummary(currentStreak,2,60,1,15,false,true,"2026-11-15","Asia/Ho_Chi_Minh"))
 override suspend fun calendar(year:Int,month:Int,session:SessionRequest)=ok(ActivityCalendar(year,month,"Asia/Ho_Chi_Minh",listOf(ActivityDay("2026-11-15",60,1,true,false,1)),1))
}
@RunWith(AndroidJUnit4::class)
class OfflineActivityTest{
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 private lateinit var db:OfflineDatabase
 private val api=ActivityTestApi();private val users=FlashUsers();private val tokens=FlashTokens()
 @get:Rule val migration=androidx.room.testing.MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),OfflineDatabase::class.java.canonicalName!!)
 @Before fun open(){context.deleteDatabase("sprint08-test.db");db=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint08-test.db").build()}
 @After fun close(){db.close();context.deleteDatabase("sprint08-test.db")}
 private fun repo()=ActivityRepository(db,api,users,tokens,Gson(),{})
 private fun event()=ActivitySegment(UUID.randomUUID().toString(),"grammar","2026-11-15T16:59:30Z",60,"Asia/Ho_Chi_Minh","pattern")
 @Test fun migrationFiveToSixPreservesOtherProducersAndAddsPrivateCache(){
  migration.createDatabase("sprint08-migration",5).apply{
   execSQL("INSERT INTO outbox VALUES('a','id','practice:x','practice','{}','pending',NULL,0)")
   execSQL("INSERT INTO practice_cache VALUES('a','draft:x','draft','{}',1)")
   close()
  }
  migration.runMigrationsAndValidate("sprint08-migration",6,true,OfflineDatabase.MIGRATION_5_6).apply{
   query("SELECT producer FROM outbox").use{assertTrue(it.moveToFirst());assertEquals("practice",it.getString(0))}
   query("SELECT payload FROM practice_cache").use{assertTrue(it.moveToFirst());assertEquals("{}",it.getString(0))}
   execSQL("INSERT INTO activity_cache VALUES('a','same','{}',1)")
   execSQL("INSERT INTO activity_cache VALUES('b','same','{}',1)")
   query("SELECT count(*) FROM activity_cache").use{assertTrue(it.moveToFirst());assertEquals(2,it.getInt(0))}
   close()
  }
 }
 @Test fun lostReplyAndReopenKeepTheSameActivityPayload():Unit=runBlocking{
  val e=event();repo().record("account-a",e);val payload=db.progress().messagesProducer("account-a","activity").single().payload
  api.lost=true;runCatching{repo().sync("account-a")}
  db.close();db=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint08-test.db").build()
  assertEquals(payload,db.progress().messagesProducer("account-a","activity").single().payload)
  assertTrue(repo().sync("account-a"));assertEquals(1,api.receipts.size);assertTrue(db.progress().messagesProducer("account-a","activity").isEmpty())
 }
 @Test fun independentProducersAndStaleAccountRepliesAreRetained():Unit=runBlocking{
  repo().record("account-a",event());db.progress().enqueue(OutboxMessage("account-a","other","practice:x","practice","{}"))
  api.duringReply={users.saveUser(users.getUser()!!.copy(id="account-b"))}
  repo().sync("account-a")
  assertEquals(1,db.progress().messagesProducer("account-a","activity").size)
  assertEquals(1,db.progress().messagesProducer("account-a","practice").size)
 }
}
