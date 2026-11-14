package com.nihonix.mobile.feature.topics.data.dto

import com.google.gson.annotations.SerializedName
import com.nihonix.mobile.feature.auth.data.dto.UserDto
import com.nihonix.mobile.feature.topics.domain.model.JlptLevel
import com.nihonix.mobile.feature.topics.domain.model.LocalizedText
import com.nihonix.mobile.feature.topics.domain.model.Topic
import com.nihonix.mobile.feature.topics.domain.model.UserLevel

data class MultiLanguageTextDto(
    @SerializedName("vi") val vi: String? = null,
    @SerializedName("en") val en: String? = null,
    @SerializedName("jp") val jp: String? = null
) {
    fun toDomain(): LocalizedText = LocalizedText(
        vi = vi.orEmpty(),
        en = en.orEmpty(),
        jp = jp.orEmpty()
    )
}

data class LevelDto(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: MultiLanguageTextDto? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(): JlptLevel = JlptLevel(
        code = code,
        name = name?.toDomain() ?: LocalizedText(),
        updatedAt = updatedAt
    )
}

data class UserLevelResponseDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("level_code") val levelCode: String? = null,
    @SerializedName("level") val level: LevelDto? = null
) {
    fun toDomain(): UserLevel = UserLevel(
        userId = userId,
        levelCode = levelCode,
        level = level?.toDomain()
    )
}

data class UpdateUserLevelRequestDto(
    @SerializedName("level_code") val levelCode: String
)

data class UpdateUserLevelResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("level_code") val levelCode: String? = null,
    @SerializedName("level") val level: LevelDto? = null,
    @SerializedName("user") val user: UserDto? = null
) {
    fun extractUpdatedLevelCode(fallback: String): String {
        return levelCode ?: user?.levelCode ?: level?.code ?: fallback
    }

    fun toUserLevelDomain(fallbackUserId: String = ""): UserLevel {
        val resolvedUserId = id ?: user?.id ?: fallbackUserId
        val resolvedLevelCode = levelCode ?: user?.levelCode ?: level?.code
        return UserLevel(
            userId = resolvedUserId,
            levelCode = resolvedLevelCode,
            level = level?.toDomain()
        )
    }
}

data class TopicDto(
    @SerializedName("id") val id: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("level_code") val levelCode: String,
    @SerializedName("title") val title: MultiLanguageTextDto? = null,
    @SerializedName("icon_svg") val iconSvg: String? = null,
    @SerializedName("icon_type") val iconType: String? = null,
    @SerializedName("icon_code") val iconCode: Int? = null,
    @SerializedName("order") val order: Int? = null,
    @SerializedName("etag") val etag: String? = null,
    @SerializedName("level") val level: LevelDto? = null
) {
    fun toDomain(): Topic = Topic(
        id = id,
        slug = slug,
        levelCode = levelCode,
        title = title?.toDomain() ?: LocalizedText(),
        iconSvg = iconSvg,
        iconType = iconType ?: "material",
        iconCode = iconCode,
        order = order ?: 1,
        etag = etag,
        level = level?.toDomain()
    )
}
