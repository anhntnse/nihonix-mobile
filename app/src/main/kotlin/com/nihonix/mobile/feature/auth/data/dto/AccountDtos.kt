package com.nihonix.mobile.feature.auth.data.dto

import com.google.gson.annotations.SerializedName
import com.nihonix.mobile.feature.auth.domain.model.LearningSettings

data class VerificationEmailDto(val email: String)
data class VerifyEmailRequestDto(val email: String, val code: String)
data class VerificationDeliveryDto(@SerializedName("retry_after_seconds") val retryAfterSeconds: Int = 120)
data class UpdateProfileRequestDto(val username: String, @SerializedName("full_name") val fullName: String)
data class AvatarResponseDto(@SerializedName("avatar_url") val avatarUrl: String)

data class LearningSettingsDto(
    @SerializedName("push_enabled") val pushEnabled: Boolean = false,
    @SerializedName("reminder_enabled") val reminderEnabled: Boolean = false,
    @SerializedName("reminder_time") val reminderTime: String = "20:00",
    @SerializedName("timezone") val timezone: String = "UTC",
    @SerializedName("daily_goal_minutes") val dailyGoalMinutes: Int = 15
) {
    fun toDomain() = LearningSettings(pushEnabled, reminderEnabled, reminderTime, timezone, dailyGoalMinutes)
    companion object {
        fun fromDomain(value: LearningSettings) = LearningSettingsDto(
            value.pushEnabled, value.reminderEnabled, value.reminderTime, value.timezone, value.dailyGoalMinutes
        )
    }
}
