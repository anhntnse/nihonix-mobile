package com.nihonix.mobile

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.topics.data.dto.LevelDto
import com.nihonix.mobile.feature.topics.data.dto.TopicDto
import com.nihonix.mobile.feature.topics.data.dto.UpdateUserLevelResponseDto
import com.nihonix.mobile.feature.topics.data.dto.UserLevelResponseDto
import com.nihonix.mobile.feature.topics.domain.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicsLevelsDtoTest {

    private val gson = Gson()

    @Test
    fun testParseLevelsListEnvelope() {
        val json = """
            {
              "success": true,
              "message": "Lấy dữ liệu thành công",
              "data": [
                {
                  "code": "N5",
                  "name": { "vi": "Sơ cấp", "en": "Beginner", "jp": "初級" },
                  "updated_at": "2026-11-14T03:32:00.000Z"
                },
                {
                  "code": "N4",
                  "name": { "vi": "Sơ trung cấp", "en": "Elementary", "jp": "初級中級" },
                  "updated_at": "2026-11-14T03:32:00.000Z"
                }
              ],
              "errors": null,
              "statusCode": 200
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponseDto<List<LevelDto>>>() {}.type
        val envelope: ApiResponseDto<List<LevelDto>> = gson.fromJson(json, type)

        assertTrue(envelope.success)
        assertNotNull(envelope.data)
        val levels = envelope.data!!.map { it.toDomain() }
        assertEquals(2, levels.size)
        assertEquals("N5", levels[0].code)
        assertEquals("Sơ cấp", levels[0].name.vi)
        assertEquals("Beginner", levels[0].name.en)
        assertEquals("初級", levels[0].name.jp)
    }

    @Test
    fun testParseTopicsListEnvelope() {
        val json = """
            {
              "success": true,
              "message": "Lấy dữ liệu thành công",
              "data": [
                {
                  "id": "2821dca6-1d84-4b6f-88ca-4aab33dc0ba5",
                  "slug": "greetings",
                  "level_code": "N5",
                  "title": {
                    "vi": "Lời chào hỏi",
                    "en": "Greetings",
                    "jp": "挨拶"
                  },
                  "icon_svg": null,
                  "icon_type": "material",
                  "icon_code": 59389,
                  "order": 1,
                  "etag": "\"a1b2c3d4\"",
                  "level": {
                    "code": "N5",
                    "name": { "vi": "Sơ cấp", "en": "Beginner", "jp": "初級" },
                    "updated_at": "2026-11-14T03:32:00.000Z"
                  }
                }
              ],
              "errors": null,
              "statusCode": 200
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponseDto<List<TopicDto>>>() {}.type
        val envelope: ApiResponseDto<List<TopicDto>> = gson.fromJson(json, type)

        assertTrue(envelope.success)
        val topic = envelope.data!!.first().toDomain()
        assertEquals("2821dca6-1d84-4b6f-88ca-4aab33dc0ba5", topic.id)
        assertEquals("greetings", topic.slug)
        assertEquals("N5", topic.levelCode)
        assertEquals("Greetings", topic.title.en)
        assertEquals("挨拶", topic.title.jp)
        assertEquals(59389, topic.iconCode)
        assertEquals(1, topic.order)
        assertEquals("N5", topic.level?.code)
    }

    @Test
    fun testParseUserLevelEnvelope_withNullAndPopulatedLevel() {
        val jsonNull = """
            {
              "success": true,
              "message": "Lấy dữ liệu thành công",
              "data": {
                "user_id": "user-123",
                "level_code": null,
                "level": null
              },
              "errors": null,
              "statusCode": 200
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponseDto<UserLevelResponseDto>>() {}.type
        val nullEnvelope: ApiResponseDto<UserLevelResponseDto> = gson.fromJson(jsonNull, type)
        val nullDomain = nullEnvelope.data!!.toDomain()
        assertEquals("user-123", nullDomain.userId)
        assertNull(nullDomain.levelCode)
        assertNull(nullDomain.level)

        val jsonSet = """
            {
              "success": true,
              "data": {
                "user_id": "user-123",
                "level_code": "N5",
                "level": {
                  "code": "N5",
                  "name": { "vi": "Sơ cấp", "en": "Beginner", "jp": "初級" },
                  "updated_at": "2026-11-14T03:32:00.000Z"
                }
              }
            }
        """.trimIndent()
        val setEnvelope: ApiResponseDto<UserLevelResponseDto> = gson.fromJson(jsonSet, type)
        val setDomain = setEnvelope.data!!.toDomain()
        assertEquals("N5", setDomain.levelCode)
        assertEquals("Beginner", setDomain.level?.name?.en)
    }

    @Test
    fun testParseUpdateUserLevel_bothDirectAndWrappedPayloads() {
        val type = object : TypeToken<ApiResponseDto<UpdateUserLevelResponseDto>>() {}.type

        // Format returned directly by UsersService.updateUserLevel (User entity with level relation)
        val directJson = """
            {
              "success": true,
              "message": "Cập nhật thành công",
              "data": {
                "id": "user-123",
                "username": "anh_nguyen",
                "email": "anh@example.com",
                "level_code": "N3",
                "level": {
                  "code": "N3",
                  "name": { "vi": "Trung cấp", "en": "Intermediate", "jp": "中級" },
                  "updated_at": "2026-11-14T03:32:00.000Z"
                }
              }
            }
        """.trimIndent()
        val directEnv: ApiResponseDto<UpdateUserLevelResponseDto> = gson.fromJson(directJson, type)
        val directDomain = directEnv.data!!.toUserLevelDomain()
        assertEquals("user-123", directDomain.userId)
        assertEquals("N3", directDomain.levelCode)
        assertEquals("Intermediate", directDomain.level?.name?.en)

        // Format illustrated in integration guide ({ "user": { ... } })
        val wrappedJson = """
            {
              "success": true,
              "message": "User level updated successfully",
              "data": {
                "user": {
                  "id": "user-456",
                  "username": "anh_nguyen",
                  "email": "anh@example.com",
                  "level_code": "N4"
                }
              }
            }
        """.trimIndent()
        val wrappedEnv: ApiResponseDto<UpdateUserLevelResponseDto> = gson.fromJson(wrappedJson, type)
        val wrappedDomain = wrappedEnv.data!!.toUserLevelDomain()
        assertEquals("user-456", wrappedDomain.userId)
        assertEquals("N4", wrappedDomain.levelCode)
        assertEquals("N4", wrappedEnv.data!!.extractUpdatedLevelCode("N5"))
    }

    @Test
    fun testLocalizedTextGetByLanguage() {
        val text = LocalizedText(vi = "Chào hỏi", en = "Greetings", jp = "挨拶")
        assertEquals("Chào hỏi", text.getByLanguage("vi"))
        assertEquals("Greetings", text.getByLanguage("en"))
        assertEquals("挨拶", text.getByLanguage("jp"))
        assertEquals("挨拶", text.getByLanguage("ja"))

        val partial = LocalizedText(vi = "Gia đình", en = "", jp = "")
        assertEquals("Gia đình", partial.getByLanguage("en"))
    }
}
