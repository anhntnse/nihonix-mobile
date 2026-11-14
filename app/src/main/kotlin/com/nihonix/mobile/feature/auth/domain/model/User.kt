package com.nihonix.mobile.feature.auth.domain.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val language: String = "en",
    val status: String = "active",
    val role: String = "user",
    val levelCode: String? = null,
    val createdAt: String? = null
)
