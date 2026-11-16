package com.nihonix.mobile.feature.learning.data

import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.topics.data.dto.TopicDto
import retrofit2.Response
import retrofit2.http.*

interface LearningApi {
    @GET("catalog/{kind}")
    suspend fun page(@Path("kind") kind: String, @Query("q") query: String?,
        @Query("topic_id") topicId: String?, @Query("level_code") level: String?,
        @Query("set_id") setId: String?, @Query("sub_topic_slug") grammarTopic: String?,
        @Query("page") page: Int, @Query("page_size") pageSize: Int = 20): Response<ApiResponseDto<LearningPageDto>>
    @GET("catalog/{kind}/{identifier}")
    suspend fun detail(@Path("kind") kind: String, @Path("identifier") identifier: String): Response<ApiResponseDto<LearningEntryDto>>
    @GET("topics")
    suspend fun topics(): Response<ApiResponseDto<List<TopicDto>>>
}
