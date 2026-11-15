package com.nihonix.mobile.feature.practice.data

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.*
import com.nihonix.mobile.feature.practice.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.util.UUID

data class StoredGeneration(val id:String,val kind:String,val payload:String,val snapshot:GenerationSnapshot?=null,val error:String?=null)
data class PracticeState(val account:String?=null,val sets:List<PracticeSet> = emptyList(),val drafts:Map<String,PracticeDraft> = emptyMap(),
 val requests:List<StoredGeneration> = emptyList(),val messages:List<OutboxMessage> = emptyList(),val quota:AiQuota?=null,val quotaFresh:Boolean=false)
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeRepository(val database:OfflineDatabase,private val api:PracticeApi,private val users:UserStorage,
 private val tokens:TokenStorage,val gson:Gson,private val schedule:(String)->Unit){
 private val network=Mutex()
 private val dao get()=database.practice()
 private val queue get()=database.progress()
 fun account()=users.getUser()?.id?.takeIf{tokens.hasAccessToken()}
 fun requestSync(){account()?.let(schedule)}
 fun observe():Flow<PracticeState> = combine(users.currentUserFlow,tokens.isAuthenticated){user,signedIn->user?.id?.takeIf{signedIn}}
  .distinctUntilChanged().flatMapLatest{owner->
   if(owner==null)flowOf(PracticeState()) else combine(dao.observe(owner),queue.observeProducer(owner,"practice")){rows,messages->
    if(account()!=owner)PracticeState() else {
     val quota=rows.find{it.key=="quota"}
     PracticeState(owner,rows.filter{it.kind=="set"}.map{gson.fromJson(it.payload,PracticeSet::class.java)},
      rows.filter{it.kind=="draft"}.map{gson.fromJson(it.payload,PracticeDraft::class.java)}.associateBy{it.sessionId},
      rows.filter{it.kind=="generation"}.sortedByDescending{it.savedAt}.map{gson.fromJson(it.payload,StoredGeneration::class.java)},messages,
      quota?.let{gson.fromJson(it.payload,AiQuota::class.java)},quota!=null&&System.currentTimeMillis()-quota.savedAt<60000)
    }
   }
  }
 private suspend fun store(owner:String,key:String,kind:String,value:Any){
  val payload=gson.toJson(value)
  check(payload.toByteArray(Charsets.UTF_8).size<1024*1024){"Practice data is too large."}
  dao.put(PracticeCache(owner,key,kind,payload,System.currentTimeMillis()))
 }
 private fun failure(response:Response<*>):Resource.Error {
  val obj=runCatching{JsonParser.parseString(response.errorBody()?.string()).asJsonObject}.getOrNull()
  val message=obj?.get("message")?.takeIf{it.isJsonPrimitive}?.asString?:"Request failed (HTTP ${response.code()})."
  val details=obj?.getAsJsonObject("errors")
  val code=details?.get("code")?.asString
  val reset=details?.get("resets_at")?.asString
  return Resource.Error(if(code=="AI_QUOTA_EXCEEDED")"AI allowance used. Resets at ${reset?:"the server reset time"}." else message,response.code())
 }
 private fun <T> data(response:Response<ApiResponseDto<T>>):T?=response.body()?.takeIf{response.isSuccessful&&it.success}?.data
 suspend fun cachedSet(owner:String,id:String):PracticeSet? =
  if(account()==owner)dao.get(owner,"set:"+id)?.let{gson.fromJson(it.payload,PracticeSet::class.java)} else null
 private suspend fun mergeSet(owner:String,incoming:PracticeSet){
  val old=cachedSet(owner,incoming.id)
  if(old?.status=="completed"&&incoming.status!="completed")return
  val value=if(incoming.exercises.isEmpty()&&old!=null) incoming.copy(exercises=old.exercises,result=incoming.result?:old.result) else incoming
  store(owner,"set:"+value.id,"set",value)
 }
 private suspend fun edit(owner:String,action:suspend ()->Unit):Resource<Unit> = try{
  database.withTransaction{check(account()==owner){"Account changed. Try again."};action();check(account()==owner)}
  Resource.Success(Unit)
 }catch(cancelled:CancellationException){throw cancelled}
 catch(error:Exception){Resource.Error(error.message?:"Could not save practice.")}
 suspend fun refreshQuota():Resource<Unit>{
  val owner=account()?:return Resource.Error("Sign in again.",401)
  val invalidated=edit(owner){dao.get(owner,"quota")?.let{dao.put(it.copy(savedAt=0))}}
  if(invalidated is Resource.Error)return invalidated
  val response=api.quota(SessionRequest(owner));if(account()!=owner)return Resource.Error("Account changed.")
  val quota=data(response)?:return failure(response)
  return edit(owner){store(owner,"quota","quota",quota)}
 }
 suspend fun loadLibrary(topic:String?=null,type:String?=null,status:String?=null,page:Int=1):Resource<PracticePage>{
  val owner=account()?:return Resource.Error("Sign in again.",401)
  val response=api.sets(topic,type,status,page,SessionRequest(owner));if(account()!=owner)return Resource.Error("Account changed.")
  val value=data(response)?:return failure(response)
  val saved=edit(owner){value.items.forEach{mergeSet(owner,it)}}
  return if(saved is Resource.Error)saved else Resource.Success(value)
 }
 suspend fun loadSet(id:String):Resource<Unit>{
  val owner=account()?:return Resource.Error("Sign in again.",401)
  val response=api.detail(id,SessionRequest(owner));if(account()!=owner)return Resource.Error("Account changed.")
  val set=data(response)?:return failure(response);check(set.id==id)
  return edit(owner){mergeSet(owner,set)}
 }
 suspend fun saveAnswers(id:String,answers:Map<String,String>,owner:String):Resource<Unit> = edit(owner){
  val set=cachedSet(owner,id)?:error("Practice is not available offline yet.")
  check(set.status!="completed"){"This practice is already completed."}
  val old=dao.get(owner,"draft:"+id)?.let{gson.fromJson(it.payload,PracticeDraft::class.java)}
  check(old?.requestId==null){"Answers are awaiting submission. Resolve that request before editing."}
  check(answers.keys.all{key->set.exercises.any{it.id==key}}&&answers.values.all{it.length<=200})
  store(owner,"draft:"+id,"draft",PracticeDraft(id,answers))
 }
 suspend fun submit(id:String,owner:String):Resource<Unit>{
  val saved=edit(owner){
   val set=cachedSet(owner,id)?:error("Open the practice first.")
   check(set.status!="completed"){"This practice is already completed."}
   val draft=dao.get(owner,"draft:"+id)?.let{gson.fromJson(it.payload,PracticeDraft::class.java)}?:PracticeDraft(id)
   if(draft.requestId!=null&&queue.message(owner,draft.requestId)!=null)return@edit
   val values=answerValues(set,draft.answers);validateAnswers(set,values)?.let{error(it)}
   val request=UUID.randomUUID().toString()
   val body=SubmitPractice(request,id,set.exercises.map{PracticeAnswer(it.id,gson.toJsonTree(values.getValue(it.id)))})
   queue.enqueue(OutboxMessage(owner,request,"practice:"+id,"practice",gson.toJson(body)))
   store(owner,"draft:"+id,"draft",draft.copy(requestId=request))
  }
  if(saved is Resource.Success)schedule(owner)
  return saved
 }
 suspend fun discardSubmission(id:String,owner:String):Resource<Unit> = network.withLock{
  edit(owner){
   for(message in queue.messagesProducer(owner,"practice").filter{it.entityKey=="practice:"+id})queue.acknowledge(owner,message.mutationId)
   dao.remove(owner,"draft:"+id)
  }
 }
 suspend fun sync(owner:String):Boolean=network.withLock{
  if(account()!=owner)return@withLock true
  repeat(50){
   val message=queue.pendingProducer(owner,"practice").firstOrNull()?:return@withLock true
   val body=gson.fromJson(message.payload,SubmitPractice::class.java)
   val response=api.submit(body,SessionRequest(owner));if(account()!=owner)return@withLock true
   val result=data(response)
   if(result==null){
    val permanent=response.code() in listOf(400,403,404,409,422)
    val error=failure(response)
    database.withTransaction{
     if(account()==owner)queue.message(owner,message.mutationId)?.let{queue.updateMessageState(owner,it.mutationId,
      if(permanent)"blocked" else "pending",error.message,it.attempts+1)}
    }
    if(permanent)return@repeat else return@withLock response.code()==401
   }
   check(result.session_id==body.session_id){"Unexpected practice result."}
   database.withTransaction{
    if(account()!=owner)return@withTransaction
    val before=cachedSet(owner,body.session_id)
    if(before!=null){
     val questions=before.exercises.map{q->result.results.find{it.exercise_id==q.id}?.let{r->q.copy(
      correct_answer=r.correct_answer,user_answer=r.user_answer,is_correct=r.is_correct,explanation=r.explanation)}?:q}
     mergeSet(owner,before.copy(status="completed",result=result,exercises=questions,correct_count=result.correct_count,accuracy=result.accuracy,total_time=result.total_time))
    }
    queue.acknowledge(owner,message.mutationId);dao.remove(owner,"draft:"+body.session_id)
   }
  }
  queue.pendingProducer(owner,"practice").isEmpty()
 }
 suspend fun generations(owner:String):List<StoredGeneration> =
  if(account()==owner)dao.all(owner).filter{it.kind=="generation"}.sortedByDescending{it.savedAt}.map{gson.fromJson(it.payload,StoredGeneration::class.java)} else emptyList()
 suspend fun startPractice(type:String,topic:String,count:Int):Resource<String>{
  if(type !in listOf("fill_blank","multiple_choice")||topic.isBlank()||count !in 1..15)return Resource.Error("Choose a topic and 1–15 questions.")
  val owner=account()?:return Resource.Error("Sign in again.",401)
  val id=UUID.randomUUID().toString()
  val stored=StoredGeneration(id,type,gson.toJson(GeneratePractice(id,topic,count)))
  val saved=edit(owner){store(owner,"generation:"+id,"generation",stored)}
  if(saved is Resource.Error)return saved
  val sent=sendGeneration(owner,stored)
  return if(sent is Resource.Error)sent else Resource.Success(id)
 }
 suspend fun startCards(folder:String,name:String,count:Int,topic:String="",level:String="N5"):Resource<String>{
  if(name.isBlank()||name.length>255||count !in 1..15||topic.length>500)return Resource.Error("Enter a name and choose 1–15 cards.")
  val owner=account()?:return Resource.Error("Sign in again.",401);val id=UUID.randomUUID().toString()
  val stored=StoredGeneration(id,"flashcards",gson.toJson(GenerateCards(id,folder,name,count,level,topic)))
  val saved=edit(owner){store(owner,"generation:"+id,"generation",stored)}
  if(saved is Resource.Error)return saved
  val sent=sendGeneration(owner,stored)
  return if(sent is Resource.Error)sent else Resource.Success(id)
 }
 private suspend fun sendGeneration(owner:String,stored:StoredGeneration):Resource<Unit>{
  val session=SessionRequest(owner)
  val response=when(stored.kind){
   "flashcards"->api.cards(gson.fromJson(stored.payload,GenerateCards::class.java),session)
   "multiple_choice"->api.choice(gson.fromJson(stored.payload,GeneratePractice::class.java),session)
   else->api.fill(gson.fromJson(stored.payload,GeneratePractice::class.java),session)
  }
  if(account()!=owner)return Resource.Error("Account changed.")
  val value=data(response)
  if(value==null){
   val error=failure(response)
   edit(owner){store(owner,"generation:"+stored.id,"generation",stored.copy(error=error.message))}
   return error
  }
  return applyGeneration(owner,stored,value)
 }
 private suspend fun applyGeneration(owner:String,stored:StoredGeneration,value:GenerationSnapshot):Resource<Unit>{
  check(value.request_id==stored.id&&value.kind==stored.kind){"Unexpected generation response."}
  return edit(owner){
   store(owner,"generation:"+stored.id,"generation",stored.copy(snapshot=value,error=null))
   if(value.status=="succeeded"&&value.kind!="flashcards"&&value.result!=null)mergeSet(owner,gson.fromJson(value.result,PracticeSet::class.java))
  }
 }
 suspend fun retryGeneration(id:String):Resource<Unit>{
  val owner=account()?:return Resource.Error("Sign in again.",401)
  val stored=generations(owner).find{it.id==id}?:return Resource.Error("Request not found.")
  return sendGeneration(owner,stored)
 }
 suspend fun checkGeneration(id:String):Resource<Unit>{
  val owner=account()?:return Resource.Error("Sign in again.",401)
  val stored=generations(owner).find{it.id==id}?:return Resource.Error("Request not found.")
  val response=api.request(id,SessionRequest(owner));if(account()!=owner)return Resource.Error("Account changed.")
  val value=data(response)?:return failure(response)
  return applyGeneration(owner,stored,value)
 }
 suspend fun cancelGeneration(id:String):Resource<Unit>{
  val owner=account()?:return Resource.Error("Sign in again.",401)
  val stored=generations(owner).find{it.id==id}?:return Resource.Error("Request not found.")
  val response=api.cancel(id,SessionRequest(owner));if(account()!=owner)return Resource.Error("Account changed.")
  return data(response)?.let{applyGeneration(owner,stored,it)}?:failure(response)
 }
 suspend fun purge(owner:String){database.withTransaction{dao.purge(owner);queue.purgeProducer(owner,"practice")}}
}
