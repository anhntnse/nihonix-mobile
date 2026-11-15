package com.nihonix.mobile

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.flashcards.data.*
import com.nihonix.mobile.feature.flashcards.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OfflineFlashcardsTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="sprint06-flashcards.db"
    @get:Rule val migration=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),OfflineDatabase::class.java.canonicalName,FrameworkSQLiteOpenHelperFactory())
    private lateinit var db:OfflineDatabase
    private val users=FlashUsers();private val tokens=FlashTokens();private val api=FlashTestApi();private val gson=Gson()
    private fun open()=Room.databaseBuilder(context,OfflineDatabase::class.java,name)
        .addMigrations(OfflineDatabase.MIGRATION_1_2,OfflineDatabase.MIGRATION_2_3,OfflineDatabase.MIGRATION_3_4).build()
    private fun repo()=FlashcardsRepository(db,api,users,tokens,gson,{})
    private lateinit var repository:FlashcardsRepository
    private val folder=FolderDraft(UUID.randomUUID().toString(),"My cards")
    private val card=CardDraft(UUID.randomUUID().toString(),CardFace("こんにちは"),CardFace("Hello"))
    private val set=SetDraft(UUID.randomUUID().toString(),folder.id,"Greetings",listOf(card))
    @Before fun setup(){context.deleteDatabase(name);db=open();repository=repo()}
    @After fun close(){db.close();context.deleteDatabase(name);context.deleteDatabase("sprint06-migration")}
    private suspend fun seed(){assertTrue(repository.saveFolder(folder) is Resource.Success);assertTrue(repository.saveSet(set) is Resource.Success)}
    @Test fun transientFailurePreservesDependencyOrder()=runBlocking {
        repository.saveFolder(folder)
        repository.saveFolder(folder.copy(name="Second local edit"))
        repository.saveSet(set)
        val before=db.progress().messagesProducer("account-a","flashcards").map{it.mutationId}
        api.failNextBeforeCommit=true
        assertFalse(repository.sync("account-a"))
        assertEquals(before,db.progress().messagesProducer("account-a","flashcards").map{it.mutationId})
        assertTrue(repository.sync("account-a"))
        assertTrue(db.progress().messagesProducer("account-a","flashcards").isEmpty())
        assertEquals("Second local edit",api.record("account-a","folder",folder.id)!!.name)
    }
    @Test fun largeAggregateSurvivesCursorWindowsReopenSyncAndCanonicalRestore()=runBlocking {
        repository.saveFolder(folder)
        val large=set.copy(cards=(1..200).map {
            CardDraft(UUID.randomUUID().toString(),CardFace("猫".repeat(2000)),CardFace("犬".repeat(2000)),it)
        })
        assertTrue(repository.saveSet(large) is Resource.Success)
        db.close();db=open();repository=repo()
        assertEquals(200,db.flashcards().cards("account-a",set.id).size)
        assertTrue(repository.sync("account-a"))
        assertEquals(200,api.record("account-a","set",set.id)!!.cards!!.size)
        assertEquals("猫".repeat(2000),api.record("account-a","set",set.id)!!.cards!!.first().front.text)
        repository.saveSet(large.copy(name="Local after sync"))
        assertTrue(repository.useServer("set",set.id) is Resource.Success)
        assertEquals(set.name,db.flashcards().set("account-a",set.id)!!.name)
        assertEquals(200,db.flashcards().cards("account-a",set.id).size)
        assertTrue(db.flashcards().payload("account-a","outbox:"+api.requests.last().second.mutation_id).isEmpty())
        repository.purge("account-a")
        assertTrue(db.flashcards().payload("account-a","server:set:"+set.id).isEmpty())
    }
    @Test fun immutableQueueSurvivesLostReplyAndDatabaseReopen()=runBlocking {
        seed()
        val before=db.progress().messagesProducer("account-a","flashcards").map{it.mutationId to it.payload}
        api.loseNextReply=true
        assertTrue(runCatching{repository.sync("account-a")}.isFailure)
        db.close();db=open();repository=repo()
        assertEquals(before,db.progress().messagesProducer("account-a","flashcards").map{it.mutationId to it.payload})
        assertTrue(repository.sync("account-a"))
        assertEquals(2,api.receipts.size)
        assertTrue(db.progress().messagesProducer("account-a","flashcards").isEmpty())
        assertEquals("Hello",gson.fromJson(db.flashcards().cards("account-a",set.id).single().back,CardFace::class.java).text)
    }
    @Test fun editDuringReplyUsesDependencyAndNeverOverwritesNewerText()=runBlocking {
        seed()
        api.duringReply={repository.saveSet(set.copy(name="Edited while sending"))}
        assertTrue(repository.sync("account-a"))
        val requests=api.requests.map{it.second}.filter{it.kind=="set"}
        assertEquals(2,requests.size);assertEquals(requests[0].mutation_id,requests[1].depends_on)
        assertEquals("Edited while sending",db.flashcards().set("account-a",set.id)!!.name)
        assertEquals("Edited while sending",api.record("account-a","set",set.id)!!.name)
    }
    @Test fun conflictRetainsLocalContentAndSaveCopyRestoresCanonicalOriginal()=runBlocking {
        seed();assertTrue(repository.sync("account-a"))
        repository.saveSet(set.copy(name="Local draft"))
        api.remoteRenameSet("account-a",set.id,"Server title")
        assertTrue(repository.sync("account-a"))
        assertEquals("Local draft",db.flashcards().set("account-a",set.id)!!.name)
        assertEquals("blocked",db.progress().messagesProducer("account-a","flashcards").single().state)
        assertTrue(repository.saveCopy("set",set.id) is Resource.Success)
        assertEquals("Server title",db.flashcards().set("account-a",set.id)!!.name)
        val copy=db.flashcards().sets("account-a").single{it.id!=set.id}
        assertEquals("Local draft",copy.name)
        assertNotEquals(card.id,db.flashcards().cards("account-a",copy.id).single().id)
        assertTrue(repository.sync("account-a"))
    }
    @Test fun folderConflictKeepsChildCardsAfterRemoteCascadeForRecovery()=runBlocking {
        seed();repository.sync("account-a")
        repository.saveFolder(folder.copy(name="Local renamed folder"))
        api.remoteDeleteFolder("account-a",folder.id)
        repository.sync("account-a")
        assertEquals(1,db.flashcards().cards("account-a",set.id).size)
        assertTrue(repository.saveCopy("folder",folder.id) is Resource.Success)
        val recovered=db.flashcards().sets("account-a").single()
        assertNotEquals(set.id,recovered.id);assertEquals(1,db.flashcards().cards("account-a",recovered.id).size)
        assertTrue(repository.sync("account-a"))
    }
    @Test fun useServerDiscardsOnlyChosenEntityAndProducer()=runBlocking {
        seed();repository.sync("account-a")
        repository.saveSet(set.copy(name="Local"))
        api.remoteRenameSet("account-a",set.id,"Server")
        repository.sync("account-a")
        db.progress().enqueue(OutboxMessage("account-a","progress-operation","vocabulary:hello","progress","{}"))
        assertTrue(repository.useServer("set",set.id) is Resource.Success)
        assertEquals("Server",db.flashcards().set("account-a",set.id)!!.name)
        assertEquals(1,db.progress().messagesProducer("account-a","progress").size)
        assertTrue(db.progress().messagesProducer("account-a","flashcards").isEmpty())
    }
    @Test fun switchingAccountDuringReplyCannotApplyOrUploadAnotherAccount()=runBlocking {
        seed()
        api.duringReply={users.saveUser(User("account-b","other","b@example.test"))}
        assertTrue(repository.sync("account-a"))
        assertEquals(2,db.progress().messagesProducer("account-a","flashcards").size)
        assertTrue(repository.observe().first().folders.isEmpty())
        val calls=api.requests.size
        repository.sync("account-a")
        assertEquals(calls,api.requests.size)
        users.saveUser(User("account-a","learner","a@example.test"))
        assertTrue(repository.sync("account-a"))
    }
    @Test fun staleEditorCannotWriteIntoNewAccount()=runBlocking {
        seed()
        users.saveUser(User("account-b","other","b@example.test"))
        assertTrue(repository.saveFolder(folder.copy(name="Stale edit"),"account-a") is Resource.Error)
        assertTrue(repository.deleteFolder(folder.id,"account-a") is Resource.Error)
        assertTrue(repository.saveCopy("folder",folder.id,"account-a") is Resource.Error)
        assertTrue(repository.useServer("folder",folder.id,"account-a") is Resource.Error)
        assertTrue(db.flashcards().folders("account-b").isEmpty())
        assertEquals("My cards",db.flashcards().folder("account-a",folder.id)!!.name)
    }
    @Test fun foreignLocalCardIdRejectsEntireReplacementBeforeAnyWrite()=runBlocking {
        seed()
        val other=set.copy(id=UUID.randomUUID().toString(),name="Other")
        assertTrue(repository.saveSet(other) is Resource.Error)
        assertNull(db.flashcards().set("account-a",other.id))
        assertEquals(1,db.flashcards().cards("account-a",set.id).size)
    }
    @Test fun queuedCascadeDeletionReconcilesWithoutResurrection()=runBlocking {
        seed();repository.deleteFolder(folder.id)
        assertTrue(db.flashcards().folder("account-a",folder.id)!!.deleted)
        assertTrue(repository.sync("account-a"))
        assertTrue(db.flashcards().folders("account-a").isEmpty())
        assertTrue(db.flashcards().sets("account-a").isEmpty())
        assertTrue(db.flashcards().cards("account-a",set.id).isEmpty())
    }
    @Test fun migrationPreservesProgressCacheOutboxAndAddsPrivateTables() {
        migration.createDatabase("sprint06-migration",2).apply {
            execSQL("INSERT INTO catalog_cache VALUES('url','payload','etag',1)")
            execSQL("INSERT INTO local_progress VALUES('a','vocabulary','hello','learned','mutation','time','topic','N5',1)")
            execSQL("INSERT INTO outbox VALUES('a','mutation','vocabulary:hello','progress','{}','pending',NULL,0)")
            close()
        }
        migration.runMigrationsAndValidate("sprint06-migration",4,true,OfflineDatabase.MIGRATION_2_3,OfflineDatabase.MIGRATION_3_4).apply {
            query("SELECT mutationId FROM outbox").use{assertTrue(it.moveToFirst());assertEquals("mutation",it.getString(0))}
            query("SELECT payload FROM catalog_cache").use{assertTrue(it.moveToFirst());assertEquals("payload",it.getString(0))}
            query("SELECT status FROM local_progress").use{assertTrue(it.moveToFirst());assertEquals("learned",it.getString(0))}
            execSQL("INSERT INTO flashcard_folders_local VALUES('a','same-id','A','',1,'0',NULL,0)")
            execSQL("INSERT INTO flashcard_folders_local VALUES('b','same-id','B','',1,'0',NULL,0)")
            query("SELECT count(*) FROM flashcard_folders_local").use{assertTrue(it.moveToFirst());assertEquals(2,it.getInt(0))}
            close()
        }
    }
    @Test fun payloadChunkBoundaryPreservesSupplementaryUnicode()=runBlocking {
        val text="x".repeat(31_999)+"🐱"+"日".repeat(40_000)
        repository.saveDraft("account-a","unicode",text)
        db.close();db=open();repository=repo()
        assertEquals(text,repository.draft("account-a","unicode"))
    }
    @Test fun draftAndAccountPurgeAreScoped()=runBlocking {
        seed()
        repository.saveDraft("account-a","editor","private draft")
        users.saveUser(User("account-b","other","b@example.test"))
        assertNull(repository.draft("account-a","editor"))
        repository.saveFolder(FolderDraft(folder.id,"B's own folder"))
        repository.purge("account-a")
        assertTrue(db.flashcards().folders("account-a").isEmpty())
        assertEquals("B's own folder",db.flashcards().folders("account-b").single().name)
        assertNull(db.flashcards().draft("account-a","editor"))
    }
}
