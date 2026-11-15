package com.nihonix.mobile.feature.practice.domain

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.nihonix.mobile.feature.flashcards.domain.CardDraft

data class AiBucket(val limit:Int,val used:Int,val reserved:Int,val remaining:Int)
data class AiProvider(val configured:Boolean)
data class AiQuota(val day:String,val resets_at:String,val timezone:String,val max_items:Int,val provider:AiProvider,val buckets:Map<String,AiBucket>)
data class QuestionText(val text:String,val hint:String?=null,val context:String?=null)
data class PracticeOption(val id:Int,val text:String)
data class PracticeQuestion(val id:String,val type:String,val question:QuestionText,val options:List<PracticeOption>?,val order:Int,
 val correct_answer:JsonElement?=null,val explanation:Map<String,String>?=null,val user_answer:JsonElement?=null,val is_correct:Boolean?=null)
data class AnswerResult(val exercise_id:String,val user_answer:JsonElement,val is_correct:Boolean,val correct_answer:JsonElement,val explanation:Map<String,String>,val time_spent:Int=0)
data class PracticeResult(val request_id:String,val session_id:String,val total_exercises:Int,val correct_count:Int,val accuracy:Int,
 val total_time:Int,val completion_id:String,val completed_at:String,val results:List<AnswerResult>)
data class PracticeSet(val id:String,val topic_id:String,val topic_title:Map<String,String>,val level_code:String,val exercise_type:String,
 val total_exercises:Int,val status:String,val exercises:List<PracticeQuestion> = emptyList(),val result:PracticeResult?=null,
 val correct_count:Int?=null,val accuracy:Int?=null,val total_time:Int?=null,val created_at:String?=null)
data class PracticePage(val items:List<PracticeSet>,val total:Int,val page:Int,val page_size:Int,val has_more:Boolean)
data class GenerationError(val code:String,val status:Int)
data class GenerationSnapshot(val request_id:String,val kind:String,val status:String,
 @com.google.gson.annotations.SerializedName("result") val resultPayload:JsonElement?,val error:GenerationError?=null) {
 val result:JsonObject? get()=resultPayload?.takeIf{it.isJsonObject}?.asJsonObject
}
data class GeneratedCards(val draft_id:String,val folder_id:String,val set_name:String,val cards:List<CardDraft>)
data class PracticeDraft(val sessionId:String,val answers:Map<String,String> = emptyMap(),val requestId:String?=null)
fun canGenerate(quota:AiQuota?,bucket:String)=quota?.provider?.configured==true&&(quota.buckets[bucket]?.remaining?:0)>0
fun validateAnswers(set:PracticeSet,answers:Map<String,Any>):String? {
 if(set.exercises.isEmpty()||set.exercises.size!=set.total_exercises||answers.keys!=set.exercises.map{it.id}.toSet())return "Answer every question before submitting."
 for(question in set.exercises){
  val value=answers[question.id]
  if(!question.options.isNullOrEmpty()){
   if(value !is Int||question.options.none{it.id==value})return "Choose an available answer."
  }else if(value !is String||value.isBlank()||value.length>200)return "Enter an answer of at most 200 characters."
 }
 return null
}
fun answerValues(set:PracticeSet,answers:Map<String,String>):Map<String,Any> = set.exercises.mapNotNull { q->
 answers[q.id]?.let { value->q.id to if(!q.options.isNullOrEmpty())(value.toIntOrNull()?:value) else value }
}.toMap()
