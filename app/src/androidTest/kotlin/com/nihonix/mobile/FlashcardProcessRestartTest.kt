package com.nihonix.mobile

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.flashcards.data.*
import com.nihonix.mobile.feature.flashcards.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

/** Execute seed/verify using the same installed APK across an actual force-stop. */
@RunWith(AndroidJUnit4::class)
class FlashcardProcessRestartTest {
    @Test fun pendingPrivateEditsAndDeletionSurviveProcessDeath():Unit=runBlocking {
        val phase=InstrumentationRegistry.getArguments().getString("restartPhase")
        assumeTrue(phase in listOf("seed","verify"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="sprint06-process-restart.db"
        val prefs=context.getSharedPreferences("sprint06-process-test",0)
        if(phase=="seed")context.deleteDatabase(name)
        val db=Room.databaseBuilder(context,OfflineDatabase::class.java,name)
            .addMigrations(OfflineDatabase.MIGRATION_1_2,OfflineDatabase.MIGRATION_2_3,OfflineDatabase.MIGRATION_3_4).build()
        val gson=Gson()
        val repository=FlashcardsRepository(db,FlashTestApi(),FlashUsers(),FlashTokens(),gson,{})
        val owner="account-a"
        if(phase=="seed") {
            val folder=FolderDraft(UUID.randomUUID().toString(),"Persisted folder")
            val set=SetDraft(UUID.randomUUID().toString(),folder.id,"Pending deletion",
                listOf(CardDraft(UUID.randomUUID().toString(),CardFace("日本語"),CardFace("Japanese"))))
            assertTrue(repository.saveFolder(folder) is Resource.Success)
            assertTrue(repository.saveSet(set) is Resource.Success)
            assertTrue(repository.deleteSet(set.id) is Resource.Success)
            repository.saveDraft(owner,"editor","Private unsaved draft")
            db.progress().enqueue(OutboxMessage(owner,"progress-sentinel","vocabulary:hello","progress","{}"))
            assertTrue(prefs.edit().putInt("pid",android.os.Process.myPid()).putString("set",set.id)
                .putString("queue",gson.toJson(db.progress().messagesProducer(owner,"flashcards").map{it.mutationId to it.payload})).commit())
            // Do not close the DB: the caller terminates the instrumentation/app process.
        } else {
            assertNotEquals(prefs.getInt("pid",-1),android.os.Process.myPid())
            val before=db.progress().messagesProducer(owner,"flashcards")
            assertEquals(3,before.size)
            assertEquals(prefs.getString("queue",null),gson.toJson(before.map{it.mutationId to it.payload}))
            val setId=prefs.getString("set",null)!!
            assertTrue(db.flashcards().set(owner,setId)!!.deleted)
            assertEquals(1,db.flashcards().cards(owner,setId).size)
            assertEquals("Private unsaved draft",repository.draft(owner,"editor"))
            assertTrue(repository.sync(owner))
            assertTrue(db.progress().messagesProducer(owner,"flashcards").isEmpty())
            assertEquals("progress-sentinel",db.progress().messagesProducer(owner,"progress").single().mutationId)
            assertNull(db.flashcards().set(owner,setId))
            assertTrue(db.flashcards().cards(owner,setId).isEmpty())
            assertEquals("Persisted folder",db.flashcards().folders(owner).single().name)
            db.close();context.deleteDatabase(name);prefs.edit().clear().commit()
        }
    }
}
