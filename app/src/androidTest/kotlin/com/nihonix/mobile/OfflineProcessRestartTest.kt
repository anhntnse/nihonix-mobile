package com.nihonix.mobile

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nihonix.mobile.core.offline.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Run phase=seed and phase=verify in separate instrumentation processes. */
@RunWith(AndroidJUnit4::class)
class OfflineProcessRestartTest {
    @Test fun committedDataSurvivesInstrumentationProcessDeath():Unit=runBlocking {
        val phase=InstrumentationRegistry.getArguments().getString("restartPhase")
        assumeTrue(phase in listOf("seed","verify"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="sprint05-process-restart.db"
        val prefs=context.getSharedPreferences("sprint05-process-test",0)
        if(phase=="seed") context.deleteDatabase(name)
        val db=Room.databaseBuilder(context,OfflineDatabase::class.java,name).addMigrations(OfflineDatabase.MIGRATION_1_2).build()
        if(phase=="seed") {
            db.withTransaction {
                db.progress().put(LocalProgress("restart-user","vocabulary","hello","learned","stable-mutation","2026-11-15T00:00:00Z","topic","N5"))
                db.progress().enqueue(OutboxMessage("restart-user","stable-mutation","vocabulary:hello","progress","{}"))
                db.catalog().put(CatalogCache("https://example.test/api/topics","cached payload","tag",1))
            }
            assertTrue(prefs.edit().putInt("pid",android.os.Process.myPid()).commit())
            // Intentionally leave the connection open; instrumentation will terminate this process.
        } else {
            assertNotEquals(prefs.getInt("pid",-1),android.os.Process.myPid())
            assertEquals("learned",db.progress().all("restart-user").single().status)
            assertEquals("stable-mutation",db.progress().pending("restart-user").single().mutationId)
            assertEquals("cached payload",db.catalog().get("https://example.test/api/topics")!!.payload)
            db.close();context.deleteDatabase(name);prefs.edit().clear().commit()
        }
    }
}
