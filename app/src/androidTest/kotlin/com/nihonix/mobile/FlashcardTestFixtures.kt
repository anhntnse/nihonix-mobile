package com.nihonix.mobile

import okhttp3.ResponseBody.Companion.toResponseBody
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.flashcards.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Response
import java.io.IOException
import java.util.UUID

internal class FlashUsers:UserStorage {
    override val currentUserFlow=MutableStateFlow<User?>(User("account-a","learner","a@example.test"))
    override fun getUser()=currentUserFlow.value
    override fun saveUser(user:User){currentUserFlow.value=user}
    override fun clearUser(){currentUserFlow.value=null}
}
internal class FlashTokens:TokenStorage {
    override val isAuthenticated=MutableStateFlow(true)
    override fun getAccessToken()=if(isAuthenticated.value)"access" else null
    override fun getRefreshToken()="refresh"
    override fun hasAccessToken()=isAuthenticated.value
    override fun saveTokens(accessToken:String,refreshToken:String){isAuthenticated.value=true}
    override fun clearTokens(){isAuthenticated.value=false}
}
internal class FlashTestApi:FlashcardsApi {
    val requests=mutableListOf<Pair<String,FlashMutation>>()
    val records=mutableMapOf<String,FlashRecord>()
    val receipts=mutableMapOf<String,FlashAck>()
    private val events=mutableListOf<Pair<String,FlashAck>>()
    private var sequence=0L
    var failNextBeforeCommit=false
    var loseNextReply=false
    var duringReply:(suspend ()->Unit)?=null
    private fun key(owner:String,kind:String,id:String)="$owner:$kind:$id"
    fun record(owner:String,kind:String,id:String)=records[key(owner,kind,id)]
    override suspend fun batch(batch:FlashBatch,session:SessionRequest):Response<ApiResponseDto<FlashBatchResult>> {
        val owner=session.userId!!
        if(failNextBeforeCommit){failNextBeforeCommit=false;return Response.error(503,"{}".toResponseBody())}
        val result=batch.mutations.map { mutation ->
            requests.add(owner to mutation)
            receipts["$owner:${mutation.mutation_id}"] ?: accept(owner,mutation).also {
                receipts["$owner:${mutation.mutation_id}"]=it;events.add(owner to it)
            }
        }
        duringReply?.invoke();duringReply=null
        if(loseNextReply){loseNextReply=false;throw IOException("Reply lost after server commit")}
        return Response.success(ApiResponseDto(success=true,data=FlashBatchResult(result)))
    }
    private fun accept(owner:String,m:FlashMutation):FlashAck {
        val version=(++sequence).toString()
        val current=record(owner,m.kind,m.id)
        val dependency=m.depends_on?.let{receipts["$owner:$it"]}
        val expected=if(m.depends_on==null)m.base_version else dependency?.takeIf{it.status=="accepted"}?.let {
            if(it.record?.kind==m.kind&&it.record.id==m.id)it.record.version else it.parent?.takeIf{p->m.kind=="folder"&&p.id==m.id}?.version
        }
        if(current?.deleted==true || expected!=(current?.version?:"0"))
            return FlashAck(m.mutation_id,m.kind,m.id,version,"conflict","Keep local or use server",current=current?.takeUnless{it.deleted})
        if(m.kind=="folder") {
            val deletedSets=if(m.action=="delete") records.values.filter{it.user_id==owner&&it.kind=="set"&&it.folder_id==m.id&&!it.deleted} else emptyList()
            val record=FlashRecord("folder",m.id,owner,version,m.action=="delete",m.name,m.description,m.order?:1)
            records[key(owner,"folder",m.id)]=record
            deletedSets.forEach{records[key(owner,"set",it.id)]=it.copy(version=version,deleted=true)}
            return FlashAck(m.mutation_id,m.kind,m.id,version,"accepted",record=record,
                deleted_ids=DeletedFlashcards(if(record.deleted)listOf(m.id) else emptyList(),deletedSets.map{it.id},deletedSets.flatMap{it.cards.orEmpty().map{card->card.id}}))
        }
        val folderId=m.folder_id?:current?.folder_id
        val parent=folderId?.let{record(owner,"folder",it)}?.takeUnless{it.deleted}
            ?: return FlashAck(m.mutation_id,m.kind,m.id,version,"rejected","Parent folder not found")
        val record=FlashRecord("set",m.id,owner,version,m.action=="delete",m.name,order=m.order?:1,folder_id=folderId,cards=m.cards.orEmpty())
        val nextParent=parent.copy(version=version)
        records[key(owner,"set",m.id)]=record;records[key(owner,"folder",parent.id)]=nextParent
        val removed=current?.cards.orEmpty().map{it.id}-record.cards.orEmpty().map{it.id}.toSet()
        return FlashAck(m.mutation_id,m.kind,m.id,version,"accepted",record=record,parent=nextParent,
            deleted_ids=DeletedFlashcards(sets=if(record.deleted)listOf(m.id) else emptyList(),cards=removed))
    }
    override suspend fun changes(after:String,session:SessionRequest,limit:Int):Response<ApiResponseDto<FlashChanges>> {
        val page=events.filter{it.first==session.userId&&it.second.version.toLong()>after.toLong()}.map{it.second}.take(limit)
        return Response.success(ApiResponseDto(success=true,data=FlashChanges(page,page.lastOrNull()?.version?:after,false)))
    }
    fun remoteRenameSet(owner:String,id:String,name:String) {
        val before=record(owner,"set",id)!!
        val m=FlashMutation(UUID.randomUUID().toString(),"set",id,"upsert",before.version,name=name,folder_id=before.folder_id,cards=before.cards)
        val ack=accept(owner,m);receipts["$owner:${m.mutation_id}"]=ack;events.add(owner to ack)
    }
    fun remoteDeleteFolder(owner:String,id:String) {
        val before=record(owner,"folder",id)!!
        val m=FlashMutation(UUID.randomUUID().toString(),"folder",id,"delete",before.version)
        val ack=accept(owner,m);receipts["$owner:${m.mutation_id}"]=ack;events.add(owner to ack)
    }
}
