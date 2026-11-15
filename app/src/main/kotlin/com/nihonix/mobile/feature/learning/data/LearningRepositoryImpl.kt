package com.nihonix.mobile.feature.learning.data

import com.google.gson.Gson
import com.nihonix.mobile.core.network.util.safeApiCall
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.learning.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearningRepositoryImpl(private val api: LearningApi, private val gson: Gson) : LearningRepository {
    override suspend fun page(kind: ContentKind, filter: LearningFilter, page: Int): Resource<LearningPage> = withContext(Dispatchers.IO) {
        safeApiCall(gson, { api.page(kind.wire,filter.query.trim().takeIf { it.isNotEmpty() },filter.topicId,filter.levelCode,
            filter.parentId.takeIf { kind == ContentKind.WORDS },filter.parentId.takeIf { kind == ContentKind.PATTERNS },page) },
            { it.domain(kind,page) })
    }
    override suspend fun detail(kind: ContentKind, identifier: String): Resource<LearningEntry> = withContext(Dispatchers.IO) {
        safeApiCall(gson,{api.detail(kind.wire,identifier)},{
            require(it.schema == 1 && it.version?.matches(Regex("[a-f0-9]{64}")) == true) { "Unsupported content response" }
            it.domain(kind)
        })
    }
    override suspend fun topics(): Resource<List<LearningTopic>> = withContext(Dispatchers.IO) {
        safeApiCall(gson,{api.topics()},{list -> list.map { LearningTopic(it.id,it.title?.toDomain() ?: com.nihonix.mobile.feature.topics.domain.model.LocalizedText(),it.levelCode) }})
    }
}
