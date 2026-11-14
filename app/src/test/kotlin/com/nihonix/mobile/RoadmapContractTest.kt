package com.nihonix.mobile

import com.google.gson.Gson
import com.nihonix.mobile.feature.topics.data.dto.LevelDto
import com.nihonix.mobile.feature.topics.domain.model.TopicBundle
import org.junit.Test
import org.junit.Assert.*

class RoadmapContractTest {
    private val gson = Gson()
    @Test fun countsMapWithoutInventingMissingCounts() {
        val level = gson.fromJson("""{"code":"N5","topic_count":4,"vocabulary_count":42,"grammar_count":8}""", LevelDto::class.java).toDomain()
        assertEquals(4, level.topicCount)
        assertEquals(42, level.vocabularyCount)
        assertEquals(8, level.grammarCount)
        assertNull(gson.fromJson("""{"code":"N4"}""", LevelDto::class.java).toDomain().topicCount)
    }
    @Test fun bundleKeepsGroupsExamplesAndLocalizedText() {
        val bundle = gson.fromJson("""{
          "schema_version":1,"version":"abc","topic":{"id":"t1","slug":"hello","level_code":"N5","title":{"en":"Greetings"}},
          "vocabulary_sets":[{"id":"set1","slug":"greetings","title":{"en":"Greetings"}}],
          "vocabulary_items":[{"slug":"hello","vocabulary_set_id":"set1","kana":"こんにちは","meaning":{"en":"Hello"},"examples":[{"sentence_jp":"こんにちは","sentence_en":"Hello"}]}],
          "grammar_sub_topics":[{"slug":"polite","title":{"en":"Polite"}}],
          "grammar_patterns":[{"slug":"desu","grammar_sub_topic_slug":"polite","pattern_jp":"です","explanation":{"en":"Polite ending"},"conjugation_rules":{"present":"です"}}]
        }""", TopicBundle::class.java)
        assertEquals("N5", bundle.topic?.levelCode)
        assertEquals("set1", bundle.vocabularySets.single().id)
        assertEquals("Hello", bundle.vocabularyItems.single().examples?.single()?.get("sentence_en"))
        assertEquals("polite", bundle.grammarSubTopics.single().slug)
        assertEquals("です", bundle.grammarPatterns.single().conjugationRules?.get("present"))
    }
}
