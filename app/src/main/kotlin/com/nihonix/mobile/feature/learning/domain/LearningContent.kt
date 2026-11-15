package com.nihonix.mobile.feature.learning.domain

import com.nihonix.mobile.feature.topics.domain.model.LocalizedText

enum class ContentKind(val wire: String, val label: String, val group: Boolean) {
    WORDS("vocabulary-items", "Words", false),
    SETS("vocabulary-sets", "Vocabulary sets", true),
    GRAMMAR_TOPICS("grammar-sub-topics", "Grammar topics", true),
    PATTERNS("grammar-patterns", "Grammar patterns", false);
    val vocabulary: Boolean get() = this == WORDS || this == SETS
    companion object { fun parse(value: String?) = entries.firstOrNull { it.wire == value } ?: SETS }
}
data class LearningExample(val jp: String, val romaji: String, val vi: String, val en: String) {
    fun translation(language: String) = when(language) { "vi" -> vi; "jp" -> jp; else -> en }
}
data class LearningEntry(
    val key: String, val slug: String, val title: LocalizedText = LocalizedText(),
    val topicId: String? = null, val topicTitle: LocalizedText = LocalizedText(), val levelCode: String = "",
    val kanji: String = "", val kana: String = "", val romaji: String = "", val wordType: String = "",
    val meaning: LocalizedText = LocalizedText(), val pattern: String = "", val patternRomaji: String = "",
    val explanation: LocalizedText = LocalizedText(), val description: String = "", val itemCount: Int? = null,
    val examples: List<LearningExample> = emptyList(), val rules: Map<String,String> = emptyMap(),
    val grammarPoints: List<String> = emptyList(), val difficulty: String = "", val version: String? = null
) {
    fun label(language: String) = pattern.ifBlank { kanji.ifBlank { kana.ifBlank { title.getByLanguage(language).ifBlank { slug } } } }
}
data class LearningFilter(val query: String = "", val topicId: String? = null, val levelCode: String? = null, val parentId: String? = null)
data class LearningPage(val items: List<LearningEntry>, val total: Int, val page: Int, val hasMore: Boolean, val version: String)
data class LearningTopic(val id: String, val title: LocalizedText, val levelCode: String)
