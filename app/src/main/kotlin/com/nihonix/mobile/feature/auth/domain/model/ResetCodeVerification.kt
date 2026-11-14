package com.nihonix.mobile.feature.auth.domain.model

data class ResetCodeVerification(
    val valid: Boolean,
    val token: String,
    val message: String
)
