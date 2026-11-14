package com.nihonix.mobile.feature.topics.domain.repository

import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.topics.domain.model.JlptLevel
import com.nihonix.mobile.feature.topics.domain.model.Topic
import com.nihonix.mobile.feature.topics.domain.model.UserLevel
import kotlinx.coroutines.flow.Flow

interface TopicsLevelsRepository {
    fun getTopicBundle(identifier: String): Flow<Resource<com.nihonix.mobile.feature.topics.domain.model.TopicBundle>> =
        kotlinx.coroutines.flow.flowOf(Resource.Error("Topic bundle unavailable"))

    fun getLevels(): Flow<Resource<List<JlptLevel>>>
    fun getLevelByCode(code: String): Flow<Resource<JlptLevel>>
    fun getUserLevel(): Flow<Resource<UserLevel>>
    fun updateUserLevel(levelCode: String): Flow<Resource<UserLevel>>
    fun getTopics(levelCode: String? = null): Flow<Resource<List<Topic>>>
    fun getTopicDetail(identifier: String): Flow<Resource<Topic>>
}
