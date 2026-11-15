package com.nihonix.mobile.feature.learning.domain

import com.nihonix.mobile.core.result.Resource

interface LearningRepository {
    suspend fun page(kind: ContentKind, filter: LearningFilter, page: Int): Resource<LearningPage>
    suspend fun detail(kind: ContentKind, identifier: String): Resource<LearningEntry>
    suspend fun topics(): Resource<List<LearningTopic>>
}
