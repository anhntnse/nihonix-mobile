package com.nihonix.mobile.feature.progress

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.learning.domain.LearningEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.UUID

data class ItemProgress(val learned:Boolean=false,val pending:Boolean=false,val error:String?=null)
interface ProgressTracker {
    fun observe(kind:String,id:String):Flow<ItemProgress>
    suspend fun mark(kind:String,entry:LearningEntry,learned:Boolean):Resource<Unit>
}
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepository(val database:OfflineDatabase,private val api:ProgressApi,private val tokens:TokenStorage,
    private val users:UserStorage,private val gson:Gson,private val schedule:(String)->Unit):ProgressTracker {
    fun account():String?=users.getUser()?.id?.takeIf { tokens.hasAccessToken() }
    private fun activeFlow()=combine(users.currentUserFlow,tokens.isAuthenticated) { user,authenticated->user?.id?.takeIf{authenticated} }.distinctUntilChanged()
    fun records():Flow<List<LocalProgress>> = activeFlow().flatMapLatest { id->if(id==null) flowOf(emptyList()) else database.progress().observe(id) }
    fun outbox():Flow<List<OutboxMessage>> = activeFlow().flatMapLatest { id->if(id==null) flowOf(emptyList()) else database.progress().observeOutbox(id) }
    override fun observe(kind:String,id:String):Flow<ItemProgress> = combine(records(),outbox()) { rows,messages->
        val row=rows.find{it.kind==kind && it.itemId==id}
        val message=messages.lastOrNull{it.entityKey=="$kind:$id"}
        ItemProgress(row?.status=="learned",message!=null,message?.error)
    }
    override suspend fun mark(kind:String,entry:LearningEntry,learned:Boolean):Resource<Unit> {
        val owner=account() ?: return Resource.Error("Sign in to save learning progress.",401)
        return save(owner,kind,entry,learned)
    }
    private suspend fun save(owner:String,kind:String,entry:LearningEntry,learned:Boolean,expectedMutation:String?=null):Resource<Unit> {
        if(kind !in listOf("vocabulary","grammar")) return Resource.Error("Unsupported learning item.")
        val dao=database.progress()
        val written=database.withTransaction {
            if(account()!=owner) return@withTransaction false
            val previous=dao.get(owner,kind,entry.slug)
            if(account()!=owner) return@withTransaction false
            if(expectedMutation!=null && (previous?.mutationId!=expectedMutation || dao.message(owner,expectedMutation)?.state!="blocked"))
                return@withTransaction false
            val now=System.currentTimeMillis()
            val old=previous?.clientUpdatedAt?.let{runCatching{Instant.parse(it).toEpochMilli()}.getOrNull()} ?: 0
            val time=Instant.ofEpochMilli(if(old>now+4*60000) now else maxOf(now,old+1)).toString()
            val mutation=ProgressMutation(UUID.randomUUID().toString(),kind,entry.slug,if(learned) "learned" else "not_learned",time)
            // Coalesce unsent edits for an item; an already submitted old ID remains safely idempotent remotely.
            dao.discardKey(owner,"$kind:${entry.slug}")
            dao.put(LocalProgress(owner,kind,entry.slug,mutation.status,mutation.mutation_id,time,entry.topicId.orEmpty(),entry.levelCode,previous?.serverVersion ?: 0))
            dao.enqueue(OutboxMessage(owner,mutation.mutation_id,"$kind:${entry.slug}","progress",gson.toJson(mutation)))
            true
        }
        if(!written) return Resource.Error("Session or progress changed. Try again.")
        schedule(owner)
        return Resource.Success(Unit,"Saved on this device")
    }
    suspend fun sync(owner:String):Boolean {
        if(account()!=owner) return true
        val dao=database.progress()
        repeat(20) {
            if(account()!=owner) return true
            val messages=dao.pending(owner)
            if(messages.isEmpty()) return@repeat
            val supported=messages.filter{it.producer=="progress"}
            if(supported.isEmpty()) return true
            val response=api.batch(ProgressBatch(supported.map{gson.fromJson(it.payload,ProgressMutation::class.java)}), com.nihonix.mobile.core.network.interceptor.SessionRequest(owner))
            if(account()!=owner) return true
            if(!response.isSuccessful) {
                val permanent=response.code() in listOf(400,403,404,409,422)
                database.withTransaction {
                    if(account()==owner) supported.forEach { original ->
                        if(dao.message(owner,original.mutationId)!=null) dao.enqueue(original.copy(
                            state=if(permanent) "blocked" else "pending",error="Sync failed (HTTP ${response.code()}).",attempts=original.attempts+1))
                    }
                }
                return response.code() in 400..499 && response.code() !in listOf(408,429)
            }
            val envelope=response.body()
            val data=envelope?.data?.takeIf{envelope.success} ?: return false
            val acks=data.getAsJsonArray("acknowledgements") ?: return false
            database.withTransaction {
                if(account()!=owner) return@withTransaction
                for(value in acks) {
                    val ack=value.asJsonObject
                    val id=ack["mutation_id"].asString
                    val message=dao.message(owner,id) ?: continue
                    if(ack["status"].asString=="rejected") {
                        dao.enqueue(message.copy(state="blocked",error=ack["message"]?.asString ?: "This update was rejected."))
                    } else {
                        dao.acknowledge(owner,id)
                        ack.getAsJsonObject("record")?.let { merge(owner,it) }
                    }
                }
            }
        }
        var cursor=dao.checkpoint(owner)?.cursor ?: "0"
        repeat(100) {
            if(account()!=owner) return true
            val response=api.changes(cursor, com.nihonix.mobile.core.network.interceptor.SessionRequest(owner))
            if(account()!=owner) return true
            if(!response.isSuccessful) return response.code() in 400..499 && response.code() !in listOf(408,429)
            val body=response.body()
            val data=body?.data?.takeIf{body.success} ?: return false
            val next=data["cursor"].asString
            database.withTransaction {
                if(account()!=owner) return@withTransaction
                for(event in data.getAsJsonArray("events")) event.asJsonObject.getAsJsonObject("record")?.let { merge(owner,it) }
                dao.checkpoint(SyncCheckpoint(owner,next))
            }
            cursor=next
            if(!data["has_more"].asBoolean) return true
        }
        return false
    }
    private suspend fun merge(owner:String,record:JsonObject) {
        if(record["user_id"].asString!=owner) return
        val dao=database.progress()
        val kind=record["item_type"].asString;val id=record["item_id"].asString
        if(dao.messages(owner).any{it.entityKey=="$kind:$id"}) return
        val version=record["version"].asLong
        if((dao.get(owner,kind,id)?.serverVersion ?: -1)>version) return
        dao.put(LocalProgress(owner,kind,id,record["status"].asString,record["mutation_id"].asString,record["client_updated_at"].asString,
            record["topic_id"].asString,record["level_code"].asString,version))
    }
    fun requestSync() { account()?.let(schedule) }
    suspend fun retryBlocked() {
        val owner=account() ?: return
        val dao=database.progress()
        for(message in dao.messages(owner).filter{it.state=="blocked"}) {
            val mutation=gson.fromJson(message.payload,ProgressMutation::class.java)
            val current=dao.get(owner,mutation.item_type,mutation.item_id) ?: continue
            save(owner,current.kind,LearningEntry(current.itemId,current.itemId,topicId=current.topicId,levelCode=current.levelCode),current.status=="learned",message.mutationId)
        }
        schedule(owner)
    }
    suspend fun discardPending() {
        val owner=account() ?: return
        database.withTransaction {
            if(account()!=owner) return@withTransaction
            val dao=database.progress()
            // Hide optimistic edits; replay server history from the start to restore canonical state.
            dao.messages(owner).forEach { dao.discardProgress(owner,it.mutationId) }
            dao.purgeOutbox(owner);dao.purgeCheckpoint(owner)
        }
        schedule(owner)
    }
    suspend fun purge(owner:String) { database.withTransaction {
        database.progress().purgeOutbox(owner);database.progress().purgeProgress(owner);database.progress().purgeCheckpoint(owner)
    } }
}
