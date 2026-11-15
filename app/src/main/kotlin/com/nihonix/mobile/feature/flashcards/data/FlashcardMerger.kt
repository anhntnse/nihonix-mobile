package com.nihonix.mobile.feature.flashcards.data

import com.google.gson.Gson
import com.nihonix.mobile.core.offline.OfflineDatabase
import com.nihonix.mobile.feature.flashcards.domain.*

/** Called inside the repository's Room transaction, after account/session validation. */
internal class FlashcardMerger(private val db:OfflineDatabase,private val gson:Gson) {
    private val dao get()=db.flashcards()
    private val payloads get()=FlashcardPayloads(dao)
    private fun atLeast(next:String,current:String)=next.toBigInteger()>=current.toBigInteger()
    private suspend fun pending(owner:String,kind:String,id:String)=db.progress().messagesProducer(owner,"flashcards")
        .any{it.entityKey=="flashcards:$kind:$id"}
    suspend fun server(owner:String,kind:String,id:String):FlashRecord? =
        dao.server(owner,kind,id)?.let{gson.fromJson(payloads.read(owner,it.payload),FlashRecord::class.java)}
    suspend fun apply(owner:String,event:FlashAck) {
        event.record?.let { merge(owner,it) }
        event.current?.let { merge(owner,it) }
        event.parent?.let { merge(owner,it) }
        for(id in event.deleted_ids?.sets.orEmpty()) {
            val folder=dao.set(owner,id)?.folderId ?: server(owner,"set",id)?.folder_id
            merge(owner,FlashRecord("set",id,owner,event.version,deleted=true,folder_id=folder))
        }
        for(id in event.deleted_ids?.folders.orEmpty()) merge(owner,FlashRecord("folder",id,owner,event.version,deleted=true))
        // Set records contain the full authoritative card list. Replacing that list handles deleted card IDs atomically.
    }
    suspend fun merge(owner:String,record:FlashRecord) {
        check(record.user_id==owner && record.kind in listOf("folder","set")) { "Unexpected flashcard owner or kind" }
        check(Regex("[0-9]{1,18}").matches(record.version)) { "Invalid flashcard version" }
        val previous=dao.server(owner,record.kind,record.id)
        if(previous!=null&&!atLeast(record.version,previous.version)) return
        dao.put(ServerCopy(owner,record.kind,record.id,record.version,payloads.store(owner,"server:"+record.kind+":"+record.id,gson.toJson(record))))
        val dirty=pending(owner,record.kind,record.id)
        if(record.kind=="folder") {
            val local=dao.folder(owner,record.id)
            if(local!=null&&!atLeast(record.version,local.version)) return
            if(dirty) { if(local!=null) dao.put(local.copy(version=record.version));return }
            if(record.deleted) {
                var childPending=false
                for(set in dao.sets(owner).filter{it.folderId==record.id}) {
                    if(pending(owner,"set",set.id)) { childPending=true;continue }
                    dao.deleteCards(owner,set.id);dao.deleteSet(owner,set.id)
                }
                if(childPending && local!=null) dao.put(local.copy(version=record.version,deleted=true,mutationId=null))
                else dao.deleteFolder(owner,record.id)
            } else {
                val name=record.name ?: error("Missing folder name")
                check(validateFolder(FolderDraft(record.id,name,record.description.orEmpty(),record.order))==null)
                dao.put(LocalFolder(owner,record.id,name,record.description.orEmpty(),record.order,record.version))
            }
        } else {
            val local=dao.set(owner,record.id)
            if(local!=null&&!atLeast(record.version,local.version)) return
            val parent=local?.folderId ?: record.folder_id
            val preserveParentDraft=record.deleted && parent!=null && pending(owner,"folder",parent)
            if(dirty || preserveParentDraft) { if(local!=null) dao.put(local.copy(version=record.version));return }
            if(record.deleted) {dao.deleteCards(owner,record.id);dao.deleteSet(owner,record.id)}
            else {
                val folder=record.folder_id ?: error("Missing parent folder")
                val name=record.name ?: error("Missing set name")
                val cards=record.cards ?: error("Missing card snapshot")
                check(validateSet(SetDraft(record.id,folder,name,cards,record.order))==null)
                dao.put(LocalSet(owner,record.id,folder,name,record.order,record.version))
                dao.deleteCards(owner,record.id)
                dao.putCards(cards.map{LocalCard(owner,it.id,record.id,gson.toJson(it.front),gson.toJson(it.back),it.order)})
            }
        }
    }
    suspend fun restore(owner:String,kind:String,id:String) {
        val canonical=server(owner,kind,id)
        if(canonical!=null) merge(owner,canonical)
        else if(kind=="set") {dao.deleteCards(owner,id);dao.deleteSet(owner,id)}
        else dao.deleteFolder(owner,id)
    }
}
