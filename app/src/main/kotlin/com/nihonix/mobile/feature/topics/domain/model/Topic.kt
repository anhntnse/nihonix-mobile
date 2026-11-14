package com.nihonix.mobile.feature.topics.domain.model

data class Topic(
    val id: String,
    val slug: String,
    val levelCode: String,
    val title: LocalizedText,
    val iconSvg: String? = null,
    val iconType: String = "material",
    val iconCode: Int? = null,
    val order: Int = 1,
    val etag: String? = null,
    val level: JlptLevel? = null
)
