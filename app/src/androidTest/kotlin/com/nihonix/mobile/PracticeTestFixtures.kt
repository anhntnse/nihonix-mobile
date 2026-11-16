package com.nihonix.mobile
import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.feature.practice.data.*
import com.nihonix.mobile.feature.practice.domain.*
import retrofit2.Response
import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody
internal class PracticeTestApi:PracticeApi{
 var set=PracticeSet("session","topic",mapOf("en" to "Animals"),"N5","fill_blank",1,"in_progress",
  exercises=listOf(PracticeQuestion("question","fill_blank",QuestionText("____"),null,1)))
 val receipts=mutableMapOf<String,PracticeResult>()
 val requests=mutableMapOf<String,GenerationSnapshot>()
 var loseReply=false;var loseGenerationReply=false;var generationCalls=0
 var quotaUnavailable=false
 var quotaExceeded=false
 var duringReply:(()->Unit)?=null
 private fun <T> ok(data:T)=Response.success(ApiResponseDto(success=true,data=data))
 override suspend fun quota(session:SessionRequest):Response<ApiResponseDto<AiQuota>> {
  if(quotaUnavailable)return Response.error(503,"{}".toResponseBody())
  val today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
  val reset=today.plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toString()
  return ok(AiQuota(today.toString(),reset,"Asia/Ho_Chi_Minh",15,AiProvider(true),
   mapOf("practice" to AiBucket(3,if(quotaExceeded)3 else 0,0,if(quotaExceeded)0 else 3),"flashcards" to AiBucket(2,0,0,2))))
 }
 override suspend fun sets(topic:String?,type:String?,status:String?,page:Int,session:SessionRequest,pageSize:Int)=ok(PracticePage(listOf(set),1,1,20,false))
 override suspend fun detail(id:String,session:SessionRequest)=ok(set)
 override suspend fun submit(body:SubmitPractice,session:SessionRequest):Response<ApiResponseDto<PracticeResult>>{
  val result=receipts.getOrPut(session.userId+":"+body.request_id){PracticeResult(body.request_id,body.session_id,1,1,100,0,"completion","2026-11-15T00:00:00Z",
   listOf(AnswerResult("question",body.answers.single().user_answer,true,if(set.exercise_type=="multiple_choice")JsonPrimitive(1) else JsonPrimitive("猫"),mapOf("en" to "Cat"))))}
  set=set.copy(status="completed",result=result)
  duringReply?.invoke();duringReply=null
  if(loseReply){loseReply=false;throw IOException("Reply lost")}
  return ok(result)
 }
 override suspend fun fill(body:GeneratePractice,session:SessionRequest):Response<ApiResponseDto<GenerationSnapshot>>{
  if(quotaExceeded)return Response.error(429,("""{"success":false,"message":"Daily allowance used","errors":{"code":"AI_QUOTA_EXCEEDED","resets_at":"2026-11-15T17:00:00Z"}}""").toResponseBody())
  val value=requests.getOrPut(session.userId+":"+body.request_id){generationCalls++;GenerationSnapshot(body.request_id,"fill_blank","succeeded",Gson().toJsonTree(set).asJsonObject)}
  if(loseGenerationReply){loseGenerationReply=false;throw IOException("Generation reply lost")}
  return ok(value)
 }
 override suspend fun choice(body:GeneratePractice,session:SessionRequest)=fill(body,session)
 override suspend fun cards(body:GenerateCards,session:SessionRequest):Response<ApiResponseDto<GenerationSnapshot>>{
  val snapshot=requests.getOrPut(session.userId+":"+body.request_id){
   generationCalls++
   val cards=(1..body.count).map{com.nihonix.mobile.feature.flashcards.domain.CardDraft(java.util.UUID.randomUUID().toString(),
    com.nihonix.mobile.feature.flashcards.domain.CardFace("猫"),com.nihonix.mobile.feature.flashcards.domain.CardFace("Cat"),it)}
   GenerationSnapshot(body.request_id,"flashcards","succeeded",Gson().toJsonTree(GeneratedCards(body.request_id,body.folder_id,body.set_name,cards)).asJsonObject)
  }
  return ok(snapshot)
 }
 override suspend fun request(id:String,session:SessionRequest)=ok(requests.getValue(session.userId+":"+id))
 override suspend fun cancel(id:String,session:SessionRequest)=ok(GenerationSnapshot(id,"fill_blank","cancelled",null))
}
