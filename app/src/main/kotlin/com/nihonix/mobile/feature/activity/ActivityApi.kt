package com.nihonix.mobile.feature.activity
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import retrofit2.Response
import retrofit2.http.*
interface ActivityApi{
 @POST("learning-streaks/activities/batch")suspend fun batch(@Body body:ActivityBatch,@Tag session:SessionRequest):Response<ApiResponseDto<ActivityAck>>
 @GET("learning-streaks")suspend fun summary(@Tag session:SessionRequest):Response<ApiResponseDto<ActivitySummary>>
 @GET("learning-streaks/calendar")suspend fun calendar(@Query("year")year:Int,@Query("month")month:Int,@Tag session:SessionRequest):Response<ApiResponseDto<ActivityCalendar>>
 @GET("users/me/settings")suspend fun preferences(@Tag session:SessionRequest):Response<ApiResponseDto<ActivityPreferences>>
}
