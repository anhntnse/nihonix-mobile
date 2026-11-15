package com.nihonix.mobile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.feature.activity.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ActivityProcessRestartTest{
 @Test fun activityIdentityAndTimezoneSurviveProcessDeath():Unit=runBlocking{
  val phase=InstrumentationRegistry.getArguments().getString("restartPhase");assumeTrue(phase in listOf("seed","verify"))
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="sprint08-restart.db";val prefs=context.getSharedPreferences("sprint08-restart",0)
  if(phase=="seed")context.deleteDatabase(name)
  val db=Room.databaseBuilder(context,OfflineDatabase::class.java,name).build()
  val api=ActivityTestApi();val repo=ActivityRepository(db,api,FlashUsers(),FlashTokens(),Gson(),{})
  if(phase=="seed"){
   val event=ActivitySegment(java.util.UUID.randomUUID().toString(),"practice","2026-11-15T16:59:30Z",60,"Asia/Ho_Chi_Minh")
   repo.record("account-a",event);repo.refresh(2026,11)
   val message=db.progress().messagesProducer("account-a","activity").single()
   db.progress().enqueue(OutboxMessage("account-a","sentinel","practice:saved","practice","{}"))
   assertTrue(prefs.edit().putInt("pid",android.os.Process.myPid()).putString("message",Gson().toJson(message)).commit())
  }else{
   assertNotEquals(prefs.getInt("pid",-1),android.os.Process.myPid())
   assertEquals(prefs.getString("message",null),Gson().toJson(db.progress().messagesProducer("account-a","activity").single()))
   assertEquals("Asia/Ho_Chi_Minh",repo.preferences("account-a")!!.timezone)
   assertTrue(repo.sync("account-a"));assertEquals(1,api.receipts.size)
   assertNotNull(db.progress().message("account-a","sentinel"))
   db.close();context.deleteDatabase(name);prefs.edit().clear().commit()
  }
 }
}
