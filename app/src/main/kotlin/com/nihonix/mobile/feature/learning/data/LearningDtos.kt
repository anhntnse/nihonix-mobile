package com.nihonix.mobile.feature.learning.data

import com.google.gson.annotations.SerializedName
import com.nihonix.mobile.feature.learning.domain.*
import com.nihonix.mobile.feature.topics.data.dto.MultiLanguageTextDto
import com.nihonix.mobile.feature.topics.data.dto.TopicDto
import com.nihonix.mobile.feature.topics.domain.model.LocalizedText

data class ExampleDto(
    @SerializedName("sentence_jp") val jp: String? = null,
    @SerializedName("sentence_romaji") val romaji: String? = null,
    @SerializedName("sentence_vi") val vi: String? = null,
    @SerializedName("sentence_en") val en: String? = null
) { fun domain() = LearningExample(jp.orEmpty(),romaji.orEmpty(),vi.orEmpty(),en.orEmpty()) }
data class LearningEntryDto(
    val id: String? = null, val slug: String? = null, val title: MultiLanguageTextDto? = null,
    val topic: TopicDto? = null, @SerializedName("level_code") val level: String? = null,
    val kanji: String? = null, val kana: String? = null, val romaji: String? = null,
    @SerializedName("word_type") val wordType: String? = null, val meaning: MultiLanguageTextDto? = null,
    @SerializedName("pattern_jp") val pattern: String? = null,
    @SerializedName("pattern_romaji") val patternRomaji: String? = null,
    val explanation: MultiLanguageTextDto? = null, val description: String? = null,
    @SerializedName("items_count") val itemCount: Int? = null,
    val examples: List<ExampleDto>? = null, @SerializedName("usage_examples") val usageExamples: List<ExampleDto>? = null,
    @SerializedName("conjugation_rules") val rules: Map<String,String>? = null,
    @SerializedName("grammar_points") val grammarPoints: List<String>? = null,
    @SerializedName("level_difficulty") val difficulty: String? = null,
    @SerializedName("schema_version") val schema: Int? = null, val version: String? = null
) {
    fun domain(kind: ContentKind): LearningEntry {
        val slug = requireNotNull(slug?.takeIf { it.isNotBlank() }) { "Content identifier is missing" }
        val key = if (kind == ContentKind.SETS) requireNotNull(id?.takeIf { it.isNotBlank() }) else slug
        val title = title?.toDomain() ?: LocalizedText()
        when(kind) {
            ContentKind.WORDS -> require(!kana.isNullOrBlank() && meaning?.toDomain()?.getByLanguage("en")?.isNotBlank() == true) { "Vocabulary content is incomplete" }
            ContentKind.PATTERNS -> require(!pattern.isNullOrBlank() && explanation?.toDomain()?.getByLanguage("en")?.isNotBlank() == true) { "Grammar content is incomplete" }
            else -> require(title.getByLanguage("en").isNotBlank()) { "Content title is missing" }
        }
        require(itemCount == null || itemCount >= 0)
        return LearningEntry(key,slug,title,topic?.id,topic?.title?.toDomain() ?: LocalizedText(),
            level ?: topic?.levelCode.orEmpty(), kanji.orEmpty(),kana.orEmpty(),romaji.orEmpty(),wordType.orEmpty(),
            meaning?.toDomain() ?: LocalizedText(),pattern.orEmpty(),patternRomaji.orEmpty(),
            explanation?.toDomain() ?: LocalizedText(),description.orEmpty(),itemCount,
            (if(kind == ContentKind.PATTERNS) usageExamples else examples).orEmpty().map { it.domain() },
            rules.orEmpty(),grammarPoints.orEmpty(),difficulty.orEmpty(),version)
    }
}
data class LearningPageDto(
    @SerializedName("schema_version") val schema: Int? = null,
    val items: List<LearningEntryDto>? = null, val total: Int? = null, val page: Int? = null,
    @SerializedName("page_size") val pageSize: Int? = null,
    @SerializedName("has_more") val hasMore: Boolean? = null, val version: String? = null
) {
    fun domain(kind: ContentKind, expectedPage: Int): LearningPage {
        require(schema == 1 && page == expectedPage && pageSize == 20 && total != null && total >= 0 && hasMore != null)
        require(version?.matches(Regex("[a-f0-9]{64}")) == true)
        val mapped = requireNotNull(items).map { it.domain(kind) }
        require(mapped.size == (total.toLong() - (expectedPage.toLong()-1)*20).coerceIn(0L,20L).toInt())
        require(mapped.map { it.key }.distinct().size == mapped.size)
        require(hasMore == (expectedPage.toLong() * 20 < total))
        return LearningPage(mapped,total,expectedPage,hasMore,version!!)
    }
}
