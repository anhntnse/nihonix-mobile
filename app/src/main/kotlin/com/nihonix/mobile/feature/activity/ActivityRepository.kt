package com.nihonix.mobile.feature.activity
import androidx.room.withTransaction
import com.google.gson.Gson
import com.nihonix.mobile.core.offline.*
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.core.storage.TokenStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.time.Instant
import java.time.ZoneId
data class ActivityState(val account:String?=null,val summary:ActivitySummary?=null,val calendars:Map<String,ActivityCalendar> = emptyMap(),
 val preferences:ActivityPreferences?=null,val messages:List<OutboxMessage> = emptyList(),val cachedAt:Long?=null)
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityRepository(val db:OfflineDatabase,private val api:ActivityApi,private val users:UserStorage,private val tokens:TokenStorage,
 val gson:Gson,private val schedule:(String)->Unit){
 private val lock=Mutex()
 fun account()=users.getUser()?.id?.takeIf{tokens.hasAccessToken()}
 fun observe():Flow<ActivityState> = combine(users.currentUserFlow,tokens.isAuthenticated){u,s->u?.id?.takeIf{s}}.distinctUntilChanged().flatMapLatest{owner->
  if(owner==null)flowOf(ActivityState())else combine(db.activity().observe(owner),db.progress().observeProducer(owner,"activity")){rows,messages->
   fun <T> value(key:String,type:Class<T>)=rows.find{it.key==key}?.let{gson.fromJson(it.payload,type)}
   if(account()!=owner)ActivityState()else ActivityState(owner,value("summary",ActivitySummary::class.java),
    rows.filter{it.key.startsWith("calendar:")}.associate{it.key.removePrefix("calendar:") to gson.fromJson(it.payload,ActivityCalendar::class.java)},
    value("preferences",ActivityPreferences::class.java),messages,rows.find{it.key=="summary"}?.savedAt)
  }
 }
 suspend fun record(owner:String,event:ActivitySegment){
  require(event.duration_seconds in 1..300&&event.activity_type in listOf("vocabulary","grammar","flashcards","practice"))
  java.util.UUID.fromString(event.id);Instant.parse(event.occurred_at);ZoneId.of(event.timezone)
  db.withTransaction{
   check(account()==owner){"Account changed"}
   val payload=gson.toJson(event)
   val old=db.progress().message(owner,event.id)
   check(old==null||old.payload==payload){"Activity ID reused"}
   if(old==null)db.progress().enqueue(OutboxMessage(owner,event.id,"activity:"+event.id,"activity",payload))
   check(account()==owner)
  }
  schedule(owner)
 }
 suspend fun preferences(owner:String):ActivityPreferences?{
  if(account()!=owner)return null
  return db.activity().get(owner,"preferences")?.let{gson.fromJson(it.payload,ActivityPreferences::class.java)}
 }
 suspend fun cachePreferences(owner:String,value:ActivityPreferences){
  db.withTransaction{if(account()==owner)db.activity().put(ActivityCache(owner,"preferences",gson.toJson(value),System.currentTimeMillis()))}
 }
 private fun <T> data(r:Response<ApiResponseDto<T>>)=r.body()?.takeIf{r.isSuccessful&&it.success}?.data
 private suspend fun cache(owner:String,key:String,value:Any){
  db.withTransaction{if(account()==owner)db.activity().put(ActivityCache(owner,key,gson.toJson(value),System.currentTimeMillis()))}
 }
 suspend fun refreshPreferences():Boolean{
  val owner=account()?:return false
  val p=data(api.preferences(SessionRequest(owner)))?:return false
  cachePreferences(owner,p);return account()==owner
 }
 suspend fun refreshSummary(owner:String):Boolean{
  if(account()!=owner)return false
  val session=SessionRequest(owner)
  val p=data(api.preferences(session))?:return false
  if(account()!=owner)return false
  cachePreferences(owner,p)
  val s=data(api.summary(session))?:return false
  cache(owner,"summary",s)
  return account()==owner
 }
 suspend fun refresh(year:Int,month:Int):Boolean{
  val owner=account()?:return false;val session=SessionRequest(owner)
  val p=data(api.preferences(session))?:return false;cachePreferences(owner,p)
  val s=data(api.summary(session))?:return false;cache(owner,"summary",s)
  val c=data(api.calendar(year,month,session))?:return false;cache(owner,"calendar:$year-$month",c)
  return account()==owner
 }
 suspend fun sync(owner:String):Boolean=lock.withLock{
  if(account()!=owner)return@withLock true
  repeat(100){
   val message=db.progress().pendingProducer(owner,"activity").firstOrNull()?:return@withLock true
   val event=gson.fromJson(message.payload,ActivitySegment::class.java)
   val r=api.batch(ActivityBatch(listOf(event)),SessionRequest(owner));if(account()!=owner)return@withLock true
   val ack=data(r)
   if(ack==null){
    val permanent=r.code() in listOf(400,403,404,409,422)
    db.withTransaction{if(account()==owner)db.progress().message(owner,message.mutationId)?.let{
     db.progress().updateMessageState(owner,it.mutationId,if(permanent)"blocked" else "pending","Activity sync failed (HTTP ${r.code()}).",it.attempts+1)
    }}
    if(!permanent)return@withLock r.code()==401
   }else{
    check(ack.accepted==listOf(event.id)){"Unexpected activity acknowledgement"}
    db.withTransaction{if(account()==owner)db.progress().acknowledge(owner,message.mutationId)}
   }
  }
  db.progress().pendingProducer(owner,"activity").isEmpty()
 }
 fun requestSync(){account()?.let(schedule)}
 suspend fun retry(id:String,owner:String){db.withTransaction{
  if(account()==owner)db.progress().message(owner,id)?.takeIf{it.producer=="activity"}?.let{db.progress().updateMessageState(owner,id,"pending",null,it.attempts)}
 };schedule(owner)}
 suspend fun discard(id:String,owner:String)=lock.withLock{db.withTransaction{
  if(account()==owner&&db.progress().message(owner,id)?.producer=="activity")db.progress().acknowledge(owner,id)
 }}
 suspend fun purge(owner:String){db.withTransaction{db.activity().purge(owner);db.progress().purgeProducer(owner,"activity")}}
}
