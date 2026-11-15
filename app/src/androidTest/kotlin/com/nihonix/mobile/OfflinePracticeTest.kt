package com.nihonix.mobile

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.practice.data.*
import com.nihonix.mobile.feature.practice.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflinePracticeTest{
 private val context=InstrumentationRegistry.getInstrumentation().targetContext
 @get:Rule val migration=androidx.room.testing.MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),OfflineDatabase::class.java.canonicalName!!,androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory())
 private lateinit var db:OfflineDatabase
 private val users=FlashUsers();private val tokens=FlashTokens();private val api=PracticeTestApi()
 private fun open()=Room.databaseBuilder(context,OfflineDatabase::class.java,"sprint07-practice.db").build()
 private fun repo()=PracticeRepository(db,api,users,tokens,Gson(),{})
 private lateinit var repository:PracticeRepository
 @Before fun setup(){context.deleteDatabase("sprint07-practice.db");db=open();repository=repo()}
 @After fun close(){db.close();context.deleteDatabase("sprint07-practice.db");context.deleteDatabase("sprint07-migration")}
 @Test fun mostRecentGenerationIsShownFirstForRecovery()=runBlocking{
  db.practice().put(PracticeCache("account-a","generation:a","generation",Gson().toJson(StoredGeneration("a","fill_blank","{}")),100))
  db.practice().put(PracticeCache("account-a","generation:z","generation",Gson().toJson(StoredGeneration("z","fill_blank","{}")),200))
  val state=repository.observe().first{it.requests.size==2}
  assertEquals("z",state.requests.first().id)
  assertEquals("z",repository.generations("account-a").first().id)
 }
 @Test fun migrationPreservesPriorOutboxAndFlashcardData(){
  migration.createDatabase("sprint07-migration",4).apply{
   execSQL("INSERT INTO outbox VALUES('a','op','vocabulary:hello','progress','{}','pending',NULL,0)")
   execSQL("INSERT INTO flashcard_folders_local VALUES('a','folder','Old folder','',1,'0',NULL,0)")
   close()
  }
  migration.runMigrationsAndValidate("sprint07-migration",5,true,OfflineDatabase.MIGRATION_4_5).apply{
   query("SELECT mutationId FROM outbox").use{assertTrue(it.moveToFirst());assertEquals("op",it.getString(0))}
   query("SELECT name FROM flashcard_folders_local").use{assertTrue(it.moveToFirst());assertEquals("Old folder",it.getString(0))}
   execSQL("INSERT INTO practice_cache VALUES('a','same','draft','a',1)")
   execSQL("INSERT INTO practice_cache VALUES('b','same','draft','b',1)")
   query("SELECT count(*) FROM practice_cache").use{assertTrue(it.moveToFirst());assertEquals(2,it.getInt(0))}
   close()
  }
 }
 @Test fun answersAndImmutableSubmissionSurviveLostReplyAndReopen()=runBlocking{
  assertTrue(repository.loadSet(api.set.id) is Resource.Success)
  repository.saveAnswers(api.set.id,mapOf("question" to "猫"),"account-a")
  assertTrue(repository.submit(api.set.id,"account-a") is Resource.Success)
  val before=db.progress().messagesProducer("account-a","practice").single()
  api.loseReply=true;assertTrue(runCatching{repository.sync("account-a")}.isFailure)
  db.close();db=open();repository=repo()
  assertEquals(before,db.progress().messagesProducer("account-a","practice").single())
  assertTrue(repository.sync("account-a"));assertEquals(1,api.receipts.size)
  assertTrue(db.progress().messagesProducer("account-a","practice").isEmpty())
  assertEquals("completed",repository.cachedSet("account-a",api.set.id)!!.status)
 }
 @Test fun duplicateTapDoesNotCreateAnotherSubmissionAndOtherProducersRemain()=runBlocking{
  repository.loadSet(api.set.id);repository.saveAnswers(api.set.id,mapOf("question" to "猫"),"account-a")
  repository.submit(api.set.id,"account-a");repository.submit(api.set.id,"account-a")
  db.progress().enqueue(OutboxMessage("account-a","sentinel","vocabulary:hello","progress","{}"))
  assertEquals(1,db.progress().messagesProducer("account-a","practice").size)
  repository.sync("account-a");assertNotNull(db.progress().message("account-a","sentinel"))
 }
 @Test fun switchingAccountRejectsStaleEditorAndDelayedResponse()=runBlocking{
  repository.loadSet(api.set.id)
  users.saveUser(User("account-b","other","b@example.test"))
  assertTrue(repository.saveAnswers(api.set.id,mapOf("question" to "猫"),"account-a") is Resource.Error)
  assertNull(repository.cachedSet("account-b",api.set.id))
  users.saveUser(User("account-a","learner","a@example.test"))
  repository.saveAnswers(api.set.id,mapOf("question" to "猫"),"account-a");repository.submit(api.set.id,"account-a")
  api.duringReply={users.saveUser(User("account-b","other","b@example.test"))}
  repository.sync("account-a");assertEquals(1,db.progress().messagesProducer("account-a","practice").size)
 }
 @Test fun generationRecoveryReusesTheOriginalRequestAndNeverSavesCards()=runBlocking{
  api.loseGenerationReply=true
  assertTrue(runCatching{repository.startPractice("fill_blank","topic",1)}.isFailure)
  val request=repository.generations("account-a").single()
  db.close();db=open();repository=repo()
  assertTrue(repository.checkGeneration(request.id) is Resource.Success)
  assertEquals(1,api.generationCalls)
  assertTrue(db.flashcards().sets("account-a").isEmpty())
 }
 @Test fun failedTransactionAndPurgeDoNotCrossAccounts()=runBlocking{
  repository.loadSet(api.set.id)
  repository.saveAnswers(api.set.id,mapOf("question" to "猫"),"account-a")
  users.saveUser(User("account-b","other","b@example.test"))
  repository.loadSet(api.set.id)
  repository.purge("account-a")
  assertNull(repository.cachedSet("account-a",api.set.id))
  assertNotNull(repository.cachedSet("account-b",api.set.id))
 }
}
