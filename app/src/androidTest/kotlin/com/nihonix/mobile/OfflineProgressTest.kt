package com.nihonix.mobile

import androidx.room.Room
import androidx.room.withTransaction
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.learning.domain.LearningEntry
import com.nihonix.mobile.feature.progress.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class OfflineProgressTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="sprint05-test.db"
    @get:Rule val migration=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
        OfflineDatabase::class.java.canonicalName,FrameworkSQLiteOpenHelperFactory())
    private lateinit var db:OfflineDatabase
    private val users=Users()
    private val tokens=Tokens()
    private val gson=Gson()
    private class Users:UserStorage {
        override val currentUserFlow=MutableStateFlow<User?>(User("account-a","learner","a@example.test"))
        override fun getUser()=currentUserFlow.value
        override fun saveUser(user:User){currentUserFlow.value=user}
        override fun clearUser(){currentUserFlow.value=null}
    }
    private class Tokens:TokenStorage {
        override val isAuthenticated=MutableStateFlow(true)
        override fun getAccessToken()=if(isAuthenticated.value) "access" else null
        override fun getRefreshToken()="refresh"
        override fun hasAccessToken()=isAuthenticated.value
        override fun saveTokens(accessToken:String,refreshToken:String){isAuthenticated.value=true}
        override fun clearTokens(){isAuthenticated.value=false}
    }
    private class Api:ProgressApi {
        var batches=mutableListOf<ProgressBatch>()
        var fail=false
        var failureCode=503
        var reject=false
        var afterSend:(suspend ()->Unit)?=null
        var cursor="0"
        fun envelope(data:String):Response<ApiResponseDto<JsonObject>> {
            return Response.success(ApiResponseDto(success=true,data=JsonParser.parseString(data).asJsonObject))
        }
        override suspend fun batch(value:ProgressBatch,session:SessionRequest):Response<ApiResponseDto<JsonObject>> {
            batches.add(value);afterSend?.invoke();afterSend=null
            if(fail) return Response.error(failureCode,"{}".toResponseBody())
            val acks=value.mutations.joinToString(",") { mutation ->
                if(reject) return@joinToString """{"mutation_id":"${mutation.mutation_id}","status":"rejected","message":"Correct device clock"}"""
                """{"mutation_id":"${mutation.mutation_id}","status":"accepted","record":{"user_id":"${session.userId}","item_type":"${mutation.item_type}","item_id":"${mutation.item_id}","status":"${mutation.status}","mutation_id":"${mutation.mutation_id}","client_updated_at":"${mutation.client_updated_at}","topic_id":"topic","level_code":"N5","version":"1"}}"""
            }
            return envelope("""{"acknowledgements":[$acks]}""")
        }
        override suspend fun changes(after:String,session:SessionRequest,limit:Int):Response<ApiResponseDto<JsonObject>> {
            cursor=after;return envelope("""{"events":[],"cursor":"1","has_more":false}""")
        }
        override suspend fun summary()=envelope("""{"topics":[],"levels":[]}""")
    }
    @Before fun setup(){context.deleteDatabase(name);db=open()}
    private fun open()=Room.databaseBuilder(context,OfflineDatabase::class.java,name).addMigrations(OfflineDatabase.MIGRATION_1_2).build()
    @After fun close(){db.close();context.deleteDatabase(name);context.deleteDatabase("sprint05-migration")}
    private fun repo(api:Api)=ProgressRepository(db,api,tokens,users,gson,{})
    private val word=LearningEntry("hello","hello",topicId="topic",levelCode="N5")
    @Test fun pendingEditAndCacheSurviveDatabaseReopenThenRetryUsesSameMutation()=runBlocking {
        val api=Api();val repository=repo(api)
        repository.mark("vocabulary",word,true)
        val id=db.progress().pending("account-a").single().mutationId
        withContext(Dispatchers.IO){db.catalog().put(CatalogCache("http://example.test/api/topics","saved","etag",1))}
        api.fail=true;assertFalse(repository.sync("account-a"))
        db.close();db=open()
        assertEquals(id,db.progress().pending("account-a").single().mutationId)
        withContext(Dispatchers.IO){assertEquals("saved",db.catalog().get("http://example.test/api/topics")!!.payload)}
        api.fail=false;assertTrue(repo(api).sync("account-a"))
        assertEquals(id,api.batches.last().mutations.single().mutation_id)
        assertTrue(db.progress().pending("account-a").isEmpty())
        assertEquals("learned",db.progress().all("account-a").single().status)
        assertEquals("1",db.progress().checkpoint("account-a")!!.cursor)
    }
    @Test fun throttledBatchRetainsItsIdAndRequestsRetry()=runBlocking {
        val api=Api();val repository=repo(api)
        repository.mark("vocabulary",word,true)
        val id=db.progress().pending("account-a").single().mutationId
        api.fail=true;api.failureCode=429
        assertFalse(repository.sync("account-a"))
        val pending=db.progress().pending("account-a").single()
        assertEquals(id,pending.mutationId);assertEquals(1,pending.attempts)
    }
    @Test fun rejectedMutationCanBeRetriedAsANewEditOrDiscarded()=runBlocking {
        val api=Api();val repository=repo(api)
        repository.mark("vocabulary",word,true)
        val id=db.progress().pending("account-a").single().mutationId
        api.reject=true;assertTrue(repository.sync("account-a"))
        assertEquals("blocked",db.progress().messages("account-a").single().state)
        repository.retryBlocked()
        assertNotEquals(id,db.progress().pending("account-a").single().mutationId)
        repository.discardPending()
        assertTrue(db.progress().messages("account-a").isEmpty())
        assertTrue(db.progress().all("account-a").isEmpty())
    }
    @Test fun summaryTracksTopicLevelPendingAndDeletedInventory()=runBlocking {
        val repository=repo(Api())
        val manifest="""{"data":{"schema_version":1,"collections":[{"kind":"topics","entries":[{"id":"topic","slug":"greetings","level_code":"N5","title":{"en":"Greetings"}}]},{"kind":"vocabulary-items","entries":[{"id":"hello","slug":"hello","topic_id":"topic"}]},{"kind":"grammar-patterns","entries":[{"id":"desu","slug":"desu","topic_id":null,"topic_slug":"greetings"}]}]}}"""
        withContext(Dispatchers.IO){db.catalog().put(CatalogCache("https://example.test/api/catalog/manifest",manifest,null,1))}
        repository.mark("vocabulary",word,true)
        val vm=ProgressViewModel(repository,db.catalog())
        val store=androidx.lifecycle.ViewModelStore()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.put("progress",vm) }
        try {
            val initial=withTimeout(5000){vm.uiState.first{it.rows.size==1}}
            assertEquals(ProgressSummary("N5",1,2),initial.levels.single());assertEquals(1,initial.pending)
            repository.mark("grammar",LearningEntry("desu","desu",topicId="topic",levelCode="N5"),true)
            val completed=withTimeout(5000){vm.uiState.first{it.levels.singleOrNull()?.learned==2 && it.pending==2}}
            assertEquals(2,completed.pending)
            val deleted=manifest.replace("""{"id":"hello","slug":"hello","topic_id":"topic"}""","")
            withContext(Dispatchers.IO){db.catalog().put(CatalogCache("https://example.test/api/catalog/manifest",deleted,null,2))}
            val pruned=withTimeout(5000){vm.uiState.first{it.levels.singleOrNull()?.total==1}}
            assertEquals(1,pruned.levels.single().learned)
        } finally { InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() } }
    }
    @Test fun retryBlockedCannotMoveAnEditIntoTheNewAccount()=runBlocking {
        db.close()
        val armed=java.util.concurrent.atomic.AtomicBoolean(false)
        db=Room.databaseBuilder(context,OfflineDatabase::class.java,name)
            .addMigrations(OfflineDatabase.MIGRATION_1_2)
            .setQueryCallback({sql,_ ->
                if(sql.contains("SELECT * FROM local_progress WHERE account") && armed.compareAndSet(true,false))
                    users.saveUser(User("account-b","other","b@example.test"))
            },java.util.concurrent.Executor { it.run() }).build()
        val repository=repo(Api())
        repository.mark("vocabulary",word,true)
        val message=db.progress().pending("account-a").single()
        db.progress().enqueue(message.copy(state="blocked"))
        armed.set(true)
        repository.retryBlocked()
        assertTrue(db.progress().messages("account-b").isEmpty())
        assertEquals(1,db.progress().messages("account-a").size)
    }
    @Test fun switchingAccountsHidesAndNeverUploadsPreviousQueue()=runBlocking {
        val api=Api();val repository=repo(api)
        repository.mark("vocabulary",word,true)
        users.saveUser(User("account-b","other","b@example.test"))
        assertTrue(repository.sync("account-a"));assertTrue(api.batches.isEmpty())
        assertTrue(repository.records().first().isEmpty())
        repository.mark("vocabulary",word,false)
        assertEquals(1,db.progress().pending("account-a").size)
        assertEquals(1,db.progress().pending("account-b").size)
        repository.purge("account-b")
        assertTrue(db.progress().all("account-b").isEmpty())
        assertEquals(1,db.progress().pending("account-a").size)
    }
    @Test fun editWhileAcknowledgementArrivesIsNotOverwritten()=runBlocking {
        val api=Api();val repository=repo(api)
        repository.mark("vocabulary",word,true)
        api.afterSend={ repository.mark("vocabulary",word,false);tokens.clearTokens() }
        assertTrue(repository.sync("account-a"))
        assertEquals("not_learned",db.progress().all("account-a").single().status)
        assertEquals(1,db.progress().pending("account-a").size)
        tokens.saveTokens("access","refresh")
        assertTrue(repository.sync("account-a"))
        assertEquals("not_learned",db.progress().all("account-a").single().status)
        assertTrue(db.progress().pending("account-a").isEmpty())
    }
    @Test fun localWriteAndOutboxRollbackTogether()=runBlocking {
        try { db.withTransactionForTest() } catch(_:IllegalStateException) {}
        assertTrue(db.progress().all("account-a").isEmpty())
        assertTrue(db.progress().pending("account-a").isEmpty())
    }
    private suspend fun OfflineDatabase.withTransactionForTest():Unit=withTransaction {
        progress().put(LocalProgress("account-a","vocabulary","hello","learned","id","time","topic","N5"))
        progress().enqueue(OutboxMessage("account-a","id","vocabulary:hello","progress","{}"))
        error("simulated process interruption before commit")
    }
    @Test fun migrationPreservesPendingMutationAndCatalog() {
        migration.createDatabase("sprint05-migration",1).apply {
            execSQL("INSERT INTO catalog_cache VALUES('url','payload','tag',1)")
            execSQL("INSERT INTO outbox VALUES('a','mutation','vocabulary:hello','progress','{}','pending',NULL,0)")
            close()
        }
        migration.runMigrationsAndValidate("sprint05-migration",2,true,OfflineDatabase.MIGRATION_1_2).apply {
            query("SELECT mutationId FROM outbox").use { assertTrue(it.moveToFirst());assertEquals("mutation",it.getString(0)) }
            query("SELECT payload FROM catalog_cache").use { assertTrue(it.moveToFirst());assertEquals("payload",it.getString(0)) }
            close()
        }
    }
}
