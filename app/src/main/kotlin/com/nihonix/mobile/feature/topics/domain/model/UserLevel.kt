package com.nihonix.mobile.feature.topics.domain.model

data class UserLevel(
    val userId: String,
    val levelCode: String?,
    val level: JlptLevel? = null
)
