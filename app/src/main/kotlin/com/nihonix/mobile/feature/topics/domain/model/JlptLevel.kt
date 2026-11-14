package com.nihonix.mobile.feature.topics.domain.model

data class JlptLevel(
    val code: String,
    val name: LocalizedText,
    val updatedAt: String? = null
)
