package com.nihonix.mobile.feature.topics.presentation

import com.nihonix.mobile.feature.topics.domain.model.JlptLevel
import com.nihonix.mobile.feature.topics.domain.model.Topic

data class TopicsUiState(
    val levels: List<JlptLevel> = emptyList(),
    val isLevelsLoading: Boolean = false,
    val levelsError: String? = null,
    val userLevelCode: String? = null,
    val selectedLevelCode: String? = null,
    val selectedLevelDetail: JlptLevel? = null,
    val isUpdatingUserLevel: Boolean = false,
    val levelUpdateMessage: String? = null,
    val levelUpdateError: String? = null,
    val topics: List<Topic> = emptyList(),
    val searchQuery: String = "",
    val isTopicsLoading: Boolean = false,
    val isTopicsEmpty: Boolean = false,
    val topicsError: String? = null,
    val notice: String? = null
) {
    val filteredTopics: List<Topic>
        get() {
            val query = searchQuery.trim()
            if (query.isEmpty()) return topics
            return topics.filter { topic ->
                topic.title.vi.contains(query, ignoreCase = true) ||
                    topic.title.en.contains(query, ignoreCase = true) ||
                    topic.title.jp.contains(query, ignoreCase = true) ||
                    topic.slug.contains(query, ignoreCase = true)
            }
        }
}

data class TopicDetailUiState(
    val bundle: com.nihonix.mobile.feature.topics.domain.model.TopicBundle? = null,
    val isBundleLoading: Boolean = false,
    val bundleError: String? = null,
    val isLoading: Boolean = false,
    val topic: Topic? = null,
    val levelDetail: JlptLevel? = null,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
    val notice: String? = null
)
