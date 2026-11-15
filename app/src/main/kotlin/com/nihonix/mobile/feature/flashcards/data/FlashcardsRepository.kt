package com.nihonix.mobile.feature.flashcards.data

import androidx.room.withTransaction
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.flashcards.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class FlashcardsState(val account:String?=null,val folders:List<LocalFolder> = emptyList(),val sets:List<LocalSet> = emptyList(),
    val cards:List<LocalCard> = emptyList(),val messages:List<OutboxMessage> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardsRepository(val database:OfflineDatabase,private val api:FlashcardsApi,private val users:UserStorage,
    private val tokens:TokenStorage,val gson:Gson,private val schedule:(String)->Unit) {
    private val network=Mutex()
    private val dao get()=database.flashcards()
    private val queue get()=database.progress()
    private val merger=FlashcardMerger(database,gson)
    private val payloads get()=FlashcardPayloads(dao)
    fun account():String?=users.getUser()?.id?.takeIf{tokens.hasAccessToken()}
    fun observe():Flow<FlashcardsState> =
        combine(users.currentUserFlow,tokens.isAuthenticated){user,signedIn->user?.id?.takeIf{signedIn}}.distinctUntilChanged()
            .flatMapLatest { owner ->
                if(owner==null) flowOf(FlashcardsState())
                else combine(dao.observeFolders(owner),dao.observeSets(owner),dao.observeCards(owner),queue.observeProducer(owner,"flashcards")) { folders,sets,cards,messages ->
                    if(account()!=owner) FlashcardsState() else FlashcardsState(owner,folders,sets,cards,messages)
                }
            }
    fun requestSync() { account()?.let(schedule) }
    fun cards(state:FlashcardsState,setId:String)=state.cards.filter{it.setId==setId}
        .map{CardDraft(it.id,gson.fromJson(it.front,CardFace::class.java),gson.fromJson(it.back,CardFace::class.java),it.position)}
    suspend fun saveFolder(draft:FolderDraft,expectedOwner:String?=null):Resource<Unit> {
        validateFolder(draft)?.let{return Resource.Error(it)}
        val owner=account()?.takeIf{expectedOwner==null||it==expectedOwner} ?: return Resource.Error("Sign in to edit flashcards.",401)
        return edit(owner) { saveFolderInside(owner,draft.copy(id=draft.id.lowercase())) }
    }
    suspend fun saveSet(draft:SetDraft,expectedOwner:String?=null):Resource<Unit> {
        validateSet(draft)?.let{return Resource.Error(it)}
        val owner=account()?.takeIf{expectedOwner==null||it==expectedOwner} ?: return Resource.Error("Sign in to edit flashcards.",401)
        return edit(owner) { saveSetInside(owner,draft.copy(id=draft.id.lowercase(),folderId=draft.folderId.lowercase(),
            cards=draft.cards.map{it.copy(id=it.id.lowercase())})) }
    }
    private suspend fun edit(owner:String,action:suspend ()->Unit):Resource<Unit> = try {
        database.withTransaction {
            check(account()==owner) { "Account changed. Please retry." }
            action()
            check(account()==owner) { "Account changed. Please retry." }
        }
        schedule(owner);Resource.Success(Unit,"Saved on this device")
    } catch(cancelled:CancellationException) { throw cancelled }
    catch(error:Exception) {Resource.Error(error.message ?: "Could not save flashcards.")}
    private suspend fun enqueue(owner:String,mutation:FlashMutation):String {
        val messages=queue.messagesProducer(owner,"flashcards")
        val relevant=messages.filter { message ->
            val old=gson.fromJson(payloads.read(owner,message.payload),FlashMutation::class.java)
            old.kind==mutation.kind&&old.id==mutation.id || mutation.kind=="folder"&&old.folder_id==mutation.id
        }
        val predecessor=relevant.lastOrNull()
        val chained=mutation.copy(depends_on=predecessor?.mutationId)
        val payload=gson.toJson(chained)
        check(payload.toByteArray(Charsets.UTF_8).size<=7*1024*1024) { "This set is too large. Shorten some text before saving." }
        queue.enqueue(OutboxMessage(owner,chained.mutation_id,"flashcards:${chained.kind}:${chained.id}","flashcards",payloads.store(owner,"outbox:"+chained.mutation_id,payload),
            state=if(relevant.any{it.state=="blocked"}) "blocked" else "pending",
            error=relevant.lastOrNull{it.state=="blocked"}?.error))
        return chained.mutation_id
    }
    private suspend fun saveFolderInside(owner:String,draft:FolderDraft) {
        val before=dao.folder(owner,draft.id)
        check(before?.deleted!=true) { "This folder was deleted. Save a new copy instead." }
        val id=enqueue(owner,FlashMutation(UUID.randomUUID().toString(),"folder",draft.id,"upsert",before?.version?:"0",
            name=draft.name.trim(),description=draft.description,order=draft.order))
        dao.put(LocalFolder(owner,draft.id,draft.name.trim(),draft.description,draft.order,before?.version?:"0",id))
    }
    private suspend fun saveSetInside(owner:String,draft:SetDraft) {
        val parent=dao.folder(owner,draft.folderId)
        check(parent!=null&&!parent.deleted) { "Folder no longer exists. Save a copy in a new folder." }
        val before=dao.set(owner,draft.id)
        check(before?.deleted!=true) { "This set was deleted. Save a new copy instead." }
        check(before==null||before.folderId==draft.folderId) { "Use Save copy to move a set." }
        check(dao.cardsByIds(owner,draft.cards.map{it.id}).none{it.setId!=draft.id}) { "A card ID belongs to another set." }
        val id=enqueue(owner,FlashMutation(UUID.randomUUID().toString(),"set",draft.id,"upsert",before?.version?:"0",
            name=draft.name.trim(),order=draft.order,folder_id=draft.folderId,cards=draft.cards))
        dao.put(LocalSet(owner,draft.id,draft.folderId,draft.name.trim(),draft.order,before?.version?:"0",id))
        dao.deleteCards(owner,draft.id)
        dao.putCards(draft.cards.map{LocalCard(owner,it.id,draft.id,gson.toJson(it.front),gson.toJson(it.back),it.order)})
    }
    suspend fun deleteFolder(id:String,expectedOwner:String?=null):Resource<Unit> {
        val owner=account()?.takeIf{expectedOwner==null||it==expectedOwner} ?: return Resource.Error("Sign in to edit flashcards.",401)
        return edit(owner) {
            val row=dao.folder(owner,id) ?: error("Folder not found")
            val mutation=enqueue(owner,FlashMutation(UUID.randomUUID().toString(),"folder",id,"delete",row.version))
            dao.put(row.copy(deleted=true,mutationId=mutation))
            for(set in dao.sets(owner).filter{it.folderId==id}) dao.put(set.copy(deleted=true))
        }
    }
    suspend fun deleteSet(id:String,expectedOwner:String?=null):Resource<Unit> {
        val owner=account()?.takeIf{expectedOwner==null||it==expectedOwner} ?: return Resource.Error("Sign in to edit flashcards.",401)
        return edit(owner) {
            val row=dao.set(owner,id) ?: error("Set not found")
            val mutation=enqueue(owner,FlashMutation(UUID.randomUUID().toString(),"set",id,"delete",row.version,folder_id=row.folderId))
            dao.put(row.copy(deleted=true,mutationId=mutation))
        }
    }
    suspend fun sync(owner:String):Boolean = network.withLock {
        if(account()!=owner)return@withLock true
        // One immutable aggregate per request bounds large-card payloads and preserves dependency order.
        for(attempt in 0 until 50) {
            if(account()!=owner)return@withLock true
            val message=queue.pendingProducer(owner,"flashcards").firstOrNull() ?: break
            val mutation=gson.fromJson(payloads.read(owner,message.payload),FlashMutation::class.java)
            val response=api.batch(FlashBatch(listOf(mutation)),SessionRequest(owner))
            if(account()!=owner)return@withLock true
            if(!response.isSuccessful) {
                val permanent=response.code() in listOf(400,403,409,413,422)
                database.withTransaction {
                    if(account()!=owner)return@withTransaction
                    queue.message(owner,message.mutationId)?.let { current ->
                        queue.updateMessageState(owner,current.mutationId,if(permanent) "blocked" else "pending",
                            "Flashcard sync failed (HTTP ${response.code()}).",current.attempts+1)
                    }
                }
                if(permanent)continue
                return@withLock response.code()==401
            }
            val envelope=response.body()
            val acks=envelope?.data?.takeIf{envelope.success}?.acknowledgements ?: return@withLock false
            val ack=acks.singleOrNull{it.mutation_id==message.mutationId} ?: return@withLock false
            check(ack.kind==mutation.kind&&ack.id==mutation.id) { "Unexpected acknowledgement target" }
            database.withTransaction {
                if(account()!=owner)return@withTransaction
                val current=queue.message(owner,message.mutationId) ?: return@withTransaction
                when(ack.status) {
                    "accepted" -> {queue.acknowledge(owner,message.mutationId);payloads.remove(owner,"outbox:"+message.mutationId)}
                    "rejected","conflict" -> queue.updateMessageState(owner,current.mutationId,"blocked",ack.message?:"Flashcards changed. Choose a server copy or save your local copy.",current.attempts)
                    else -> error("Unexpected acknowledgement status")
                }
                merger.apply(owner,ack)
            }
        }
        var cursor=dao.checkpoint(owner)?.cursor?:"0"
        for(page in 0 until 100) {
            if(account()!=owner)return@withLock true
            val response=api.changes(cursor,SessionRequest(owner))
            if(account()!=owner)return@withLock true
            if(!response.isSuccessful)return@withLock response.code() in listOf(400,401,403)
            val envelope=response.body()
            val data=envelope?.data?.takeIf{envelope.success} ?: return@withLock false
            check(data.cursor.toBigInteger()>=cursor.toBigInteger())
            if(data.has_more&&data.events.isEmpty())return@withLock false
            database.withTransaction {
                if(account()!=owner)return@withTransaction
                data.events.forEach{merger.apply(owner,it)}
                dao.checkpoint(FlashcardCheckpoint(owner,data.cursor))
            }
            cursor=data.cursor
            if(!data.has_more)return@withLock queue.pendingProducer(owner,"flashcards").isEmpty()
        }
        false
    }
    suspend fun useServer(kind:String,id:String,expectedOwner:String?=null):Resource<Unit> = network.withLock {
        val owner=account()?.takeIf{expectedOwner==null||it==expectedOwner} ?: return@withLock Resource.Error("Sign in to resolve flashcards.",401)
        edit(owner) { discardInside(owner,kind,id) }
    }
    private suspend fun discardInside(owner:String,kind:String,id:String) {
        val sets=dao.sets(owner).filter{it.folderId==id}.map{it.id}.toSet()
        for(message in queue.messagesProducer(owner,"flashcards")) {
            val m=gson.fromJson(payloads.read(owner,message.payload),FlashMutation::class.java)
            if(m.kind==kind&&m.id==id || kind=="folder"&&(m.folder_id==id||m.kind=="set"&&m.id in sets))
                {queue.acknowledge(owner,message.mutationId);payloads.remove(owner,"outbox:"+message.mutationId)}
        }
        if(kind=="folder") {
            for(setId in sets)merger.restore(owner,"set",setId)
            merger.restore(owner,"folder",id)
            // Restore server children not present in an optimistic local folder snapshot.
            dao.servers(owner).filter{it.kind=="set"}.forEach { copy ->
                val record=gson.fromJson(payloads.read(owner,copy.payload),FlashRecord::class.java)
                if(record.folder_id==id)merger.restore(owner,"set",record.id)
            }
        } else merger.restore(owner,kind,id)
    }
    suspend fun saveCopy(kind:String,id:String,expectedOwner:String?=null):Resource<Unit> = network.withLock {
        val owner=account()?.takeIf{expectedOwner==null||it==expectedOwner} ?: return@withLock Resource.Error("Sign in to recover flashcards.",401)
        edit(owner) {
            val originalFolder=if(kind=="folder")dao.folder(owner,id) else dao.set(owner,id)?.let{dao.folder(owner,it.folderId)}
            val newFolder=UUID.randomUUID().toString()
            saveFolderInside(owner,FolderDraft(newFolder,((originalFolder?.name?:"Recovered flashcards")+" (copy)").take(255),originalFolder?.description.orEmpty()))
            val originals=if(kind=="folder")dao.sets(owner).filter{it.folderId==id} else listOfNotNull(dao.set(owner,id))
            for(set in originals) {
                val cards=dao.cards(owner,set.id).map { card ->
                    CardDraft(UUID.randomUUID().toString(),gson.fromJson(card.front,CardFace::class.java),gson.fromJson(card.back,CardFace::class.java),card.position)
                }
                saveSetInside(owner,SetDraft(UUID.randomUUID().toString(),newFolder,set.name,cards,set.position))
            }
            discardInside(owner,kind,id)
        }
    }
    suspend fun saveDraft(owner:String,id:String,payload:String) {
        database.withTransaction {if(account()==owner)dao.draft(EditorDraft(owner,id,payloads.store(owner,"draft:"+id,payload)))}
    }
    suspend fun draft(owner:String,id:String):String?=database.withTransaction {
        if(account()==owner)dao.draft(owner,id)?.let{payloads.read(owner,it.payload)} else null
    }
    suspend fun clearDraft(owner:String,id:String) { database.withTransaction {
        if(account()==owner){dao.deleteDraft(owner,id);payloads.remove(owner,"draft:"+id)}
    } }
    suspend fun purge(owner:String) { database.withTransaction {
        dao.purgeCards(owner);dao.purgeSets(owner);dao.purgeFolders(owner);dao.purgeServers(owner);dao.purgeCheckpoint(owner);dao.purgeDrafts(owner);dao.purgePayloads(owner)
        queue.purgeProducer(owner,"flashcards")
    } }
}
