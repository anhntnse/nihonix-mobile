package com.nihonix.mobile.feature.practice.data

import com.google.gson.JsonElement
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.feature.practice.domain.*
import retrofit2.Response
import retrofit2.http.*

data class GeneratePractice(val request_id:String,val topic_id:String,val exercise_count:Int)
data class GenerateCards(val request_id:String,val folder_id:String,val set_name:String,val count:Int,
 val level_code:String="N5",val topic:String="",val custom_prompt:String="")
data class PracticeAnswer(val exercise_id:String,val user_answer:JsonElement,val time_spent:Int=0)
data class SubmitPractice(val request_id:String,val session_id:String,val answers:List<PracticeAnswer>)
interface PracticeApi {
 @GET("ai/quota") suspend fun quota(@Tag session:SessionRequest):Response<ApiResponseDto<AiQuota>>
 @GET("ai/requests/{id}") suspend fun request(@Path("id") id:String,@Tag session:SessionRequest):Response<ApiResponseDto<GenerationSnapshot>>
 @POST("ai/requests/{id}/cancel") suspend fun cancel(@Path("id") id:String,@Tag session:SessionRequest):Response<ApiResponseDto<GenerationSnapshot>>
 @POST("practice/fill-blank") suspend fun fill(@Body body:GeneratePractice,@Tag session:SessionRequest):Response<ApiResponseDto<GenerationSnapshot>>
 @POST("practice/multiple-choice") suspend fun choice(@Body body:GeneratePractice,@Tag session:SessionRequest):Response<ApiResponseDto<GenerationSnapshot>>
 @POST("flashcards/generate") suspend fun cards(@Body body:GenerateCards,@Tag session:SessionRequest):Response<ApiResponseDto<GenerationSnapshot>>
 @GET("practice/sets") suspend fun sets(@Query("topic_id") topic:String?,@Query("exercise_type") type:String?,@Query("status") status:String?,
  @Query("page") page:Int,@Tag session:SessionRequest,@Query("page_size") pageSize:Int=20):Response<ApiResponseDto<PracticePage>>
 @GET("practice/sets/{id}") suspend fun detail(@Path("id") id:String,@Tag session:SessionRequest):Response<ApiResponseDto<PracticeSet>>
 @POST("practice/submit") suspend fun submit(@Body body:SubmitPractice,@Tag session:SessionRequest):Response<ApiResponseDto<PracticeResult>>
}
