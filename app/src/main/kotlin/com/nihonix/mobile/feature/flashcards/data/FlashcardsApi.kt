package com.nihonix.mobile.feature.flashcards.data

import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import com.nihonix.mobile.feature.flashcards.domain.CardDraft
import retrofit2.Response
import retrofit2.http.*

data class FlashMutation(val mutation_id:String,val kind:String,val id:String,val action:String,val base_version:String,
    val depends_on:String?=null,val name:String?=null,val description:String?=null,val order:Int?=null,
    val folder_id:String?=null,val cards:List<CardDraft>?=null)
data class FlashBatch(val mutations:List<FlashMutation>)
data class DeletedFlashcards(val folders:List<String> = emptyList(),val sets:List<String> = emptyList(),val cards:List<String> = emptyList())
data class FlashRecord(val kind:String,val id:String,val user_id:String,val version:String,val deleted:Boolean=false,
    val name:String?=null,val description:String?=null,val order:Int=1,val folder_id:String?=null,val cards:List<CardDraft>?=null)
data class FlashAck(val mutation_id:String,val kind:String,val id:String,val version:String,val status:String,
    val message:String?=null,val record:FlashRecord?=null,val current:FlashRecord?=null,val parent:FlashRecord?=null,
    val deleted_ids:DeletedFlashcards?=null)
data class FlashBatchResult(val acknowledgements:List<FlashAck>)
data class FlashChanges(val events:List<FlashAck>,val cursor:String,val has_more:Boolean)
interface FlashcardsApi {
    @POST("flashcards/sync/batch")
    suspend fun batch(@Body batch:FlashBatch,@Tag session:SessionRequest):Response<ApiResponseDto<FlashBatchResult>>
    @GET("flashcards/sync/changes")
    suspend fun changes(@Query("after") after:String,@Tag session:SessionRequest,@Query("limit") limit:Int=100):Response<ApiResponseDto<FlashChanges>>
}
