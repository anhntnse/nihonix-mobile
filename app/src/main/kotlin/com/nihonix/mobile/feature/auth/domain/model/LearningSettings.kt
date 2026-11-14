package com.nihonix.mobile.feature.auth.domain.model

data class LearningSettings(
    val pushEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val timezone: String = "UTC",
    val dailyGoalMinutes: Int = 15
)
