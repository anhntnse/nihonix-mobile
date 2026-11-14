package com.nihonix.mobile.feature.topics.domain.model

import com.google.gson.annotations.SerializedName

data class CatalogEntry(
    val slug: String = "",
    val title: LocalizedText? = null,
    val id: String? = null,
    val order: Int = 1,
    @SerializedName("topic_id") val topicId: String? = null,
    @SerializedName("topic_slug") val topicSlug: String? = null,
    @SerializedName("level_code") val levelCode: String? = null,
    @SerializedName("word_type") val wordType: String? = null,
    @SerializedName("pattern_romaji") val patternRomaji: String? = null,
    @SerializedName("grammar_points") val grammarPoints: List<String>? = null,
    @SerializedName("level_difficulty") val levelDifficulty: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val description: String? = null,
    val romaji: String? = null,
    val examples: List<Map<String, String>>? = null,
    @SerializedName("usage_examples") val usageExamples: List<Map<String, String>>? = null,
    @SerializedName("conjugation_rules") val conjugationRules: Map<String, String>? = null,
    val kana: String? = null,
    val kanji: String? = null,
    val meaning: LocalizedText? = null,
    @SerializedName("pattern_jp") val pattern: String? = null,
    val explanation: LocalizedText? = null,
    @SerializedName("vocabulary_set_id") val vocabularySetId: String? = null,
    @SerializedName("grammar_sub_topic_slug") val grammarSubTopicSlug: String? = null
)
data class TopicBundle(
    @SerializedName("schema_version") val schemaVersion: Int = 0,
    val version: String = "",
    val topic: BundledTopic? = null,
    @SerializedName("vocabulary_sets") val vocabularySets: List<CatalogEntry> = emptyList(),
    @SerializedName("grammar_sub_topics") val grammarSubTopics: List<CatalogEntry> = emptyList(),
    @SerializedName("vocabulary_items") val vocabularyItems: List<CatalogEntry> = emptyList(),
    @SerializedName("grammar_patterns") val grammarPatterns: List<CatalogEntry> = emptyList()
)
