package com.nihonix.mobile.feature.topics.data.api

import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.topics.data.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface TopicsLevelsApiService {
    @GET("topics/{identifier}/bundle")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun getTopicBundle(@Path("identifier") identifier: String):
        Response<ApiResponseDto<com.nihonix.mobile.feature.topics.domain.model.TopicBundle>>


    @GET("levels")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun getLevels(): Response<ApiResponseDto<List<LevelDto>>>

    @GET("levels/{code}")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun getLevelByCode(
        @Path("code") code: String
    ): Response<ApiResponseDto<LevelDto>>

    @GET("users/me/level")
    suspend fun getUserLevel(): Response<ApiResponseDto<UserLevelResponseDto>>

    @PATCH("users/me/level")
    suspend fun updateUserLevel(
        @Body body: UpdateUserLevelRequestDto
    ): Response<ApiResponseDto<UpdateUserLevelResponseDto>>

    @GET("topics")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun getTopics(
        @Query("levelCode") levelCode: String? = null
    ): Response<ApiResponseDto<List<TopicDto>>>

    @GET("topics/{identifier}")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun getTopicDetail(
        @Path("identifier") identifier: String
    ): Response<ApiResponseDto<TopicDto>>
}
