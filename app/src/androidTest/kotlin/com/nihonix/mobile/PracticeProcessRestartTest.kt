package com.nihonix.mobile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.practice.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class PracticeProcessRestartTest{
 @Test fun submissionAndGenerationIdentitySurviveProcessDeath():Unit=runBlocking{
  val phase=InstrumentationRegistry.getArguments().getString("restartPhase")
  assumeTrue(phase in listOf("seed","verify"))
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val name="sprint07-restart.db";val prefs=context.getSharedPreferences("sprint07-restart",0);val gson=Gson()
  if(phase=="seed")context.deleteDatabase(name)
  val db=Room.databaseBuilder(context,OfflineDatabase::class.java,name)
   .addMigrations(OfflineDatabase.MIGRATION_1_2,OfflineDatabase.MIGRATION_2_3,OfflineDatabase.MIGRATION_3_4,OfflineDatabase.MIGRATION_4_5).build()
  val api=PracticeTestApi();val repo=PracticeRepository(db,api,FlashUsers(),FlashTokens(),gson,{})
  if(phase=="seed"){
   repo.loadSet(api.set.id);repo.saveAnswers(api.set.id,mapOf("question" to "猫"),"account-a");repo.submit(api.set.id,"account-a")
   api.loseGenerationReply=true;runCatching{repo.startPractice("fill_blank","topic",1)}
   val pending=db.progress().messagesProducer("account-a","practice").single()
   val request=repo.generations("account-a").single()
   db.progress().enqueue(OutboxMessage("account-a","sentinel","vocabulary:hello","progress","{}"))
   assertTrue(prefs.edit().putInt("pid",android.os.Process.myPid()).putString("submission",gson.toJson(pending))
    .putString("generation",gson.toJson(request)).commit())
  }else{
   assertNotEquals(prefs.getInt("pid",-1),android.os.Process.myPid())
   assertEquals(prefs.getString("submission",null),gson.toJson(db.progress().messagesProducer("account-a","practice").single()))
   assertEquals(prefs.getString("generation",null),gson.toJson(repo.generations("account-a").single()))
   val body=gson.fromJson(db.progress().messagesProducer("account-a","practice").single().payload,SubmitPractice::class.java)
   assertEquals("猫",body.answers.single().user_answer.asString)
   assertTrue(repo.sync("account-a"));assertTrue(db.progress().messagesProducer("account-a","practice").isEmpty())
   assertNotNull(db.progress().message("account-a","sentinel"));assertEquals(0,api.generationCalls)
   db.close();context.deleteDatabase(name);prefs.edit().clear().commit()
  }
 }
}
