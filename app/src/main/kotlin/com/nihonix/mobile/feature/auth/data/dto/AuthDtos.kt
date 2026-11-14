package com.nihonix.mobile.feature.auth.data.dto

import com.google.gson.annotations.SerializedName
import com.nihonix.mobile.feature.auth.domain.model.AuthTokens
import com.nihonix.mobile.feature.auth.domain.model.ResetCodeVerification
import com.nihonix.mobile.feature.auth.domain.model.User

// --- Requests ---

data class RegisterRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String? = null
)

data class LoginRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class LogoutRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class ForgotPasswordRequestDto(
    @SerializedName("email") val email: String
)

data class VerifyResetCodeRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String
)

data class ResetPasswordRequestDto(
    @SerializedName("token") val token: String,
    @SerializedName("password") val password: String
)

data class ResendCodeRequestDto(
    @SerializedName("email") val email: String
)

// --- Responses & Entities ---

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("language") val language: String? = "en",
    @SerializedName("status") val status: String? = "active",
    @SerializedName("role") val role: String? = "user",
    @SerializedName("level_code") val levelCode: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    fun toDomain(): User = User(
        id = id,
        username = username,
        email = email,
        fullName = fullName,
        avatarUrl = avatarUrl,
        language = language ?: "en",
        status = status ?: "active",
        role = role ?: "user",
        levelCode = levelCode,
        createdAt = createdAt
    )
}

data class RegisterResponseDto(
    @SerializedName("user") val user: UserDto
)

data class LoginResponseDto(
    @SerializedName("user") val user: UserDto,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
) {
    fun toTokensDomain(): AuthTokens = AuthTokens(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}

data class RefreshTokenResponseDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
) {
    fun toTokensDomain(): AuthTokens = AuthTokens(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}

data class MessageResponseDto(
    @SerializedName("message") val message: String? = null
)

data class VerifyResetCodeResponseDto(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("token") val token: String,
    @SerializedName("message") val message: String
) {
    fun toDomain(): ResetCodeVerification = ResetCodeVerification(
        valid = valid,
        token = token,
        message = message
    )
}
