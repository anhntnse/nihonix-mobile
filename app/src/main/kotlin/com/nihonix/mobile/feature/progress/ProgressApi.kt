package com.nihonix.mobile.feature.progress

import com.google.gson.JsonObject
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import retrofit2.Response
import retrofit2.http.*

data class ProgressMutation(val mutation_id:String,val item_type:String,val item_id:String,val status:String,val client_updated_at:String)
data class ProgressBatch(val mutations:List<ProgressMutation>)
interface ProgressApi {
    @POST("progress/batch") suspend fun batch(@Body value:ProgressBatch, @Tag session:com.nihonix.mobile.core.network.interceptor.SessionRequest):Response<ApiResponseDto<JsonObject>>
    @GET("progress/changes") suspend fun changes(@Query("after") after:String,@Tag session:com.nihonix.mobile.core.network.interceptor.SessionRequest,@Query("limit") limit:Int=100):Response<ApiResponseDto<JsonObject>>
    @GET("progress/summary") suspend fun summary():Response<ApiResponseDto<JsonObject>>
}
