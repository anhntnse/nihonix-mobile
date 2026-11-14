package com.nihonix.mobile

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import org.junit.Assert.*
import org.junit.Test

class ApiResponseDtoTest {

    private val gson = Gson()

    @Test
    fun testParseSuccessEnvelope() {
        val json = """
            {
                "success": true,
                "message": "Lấy dữ liệu thành công",
                "data": {
                    "username": "anh_nguyen",
                    "role": "user"
                },
                "errors": null,
                "statusCode": 200
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponseDto<Map<String, String>>>() {}.type
        val envelope: ApiResponseDto<Map<String, String>> = gson.fromJson(json, type)

        assertTrue(envelope.success)
        assertEquals("Lấy dữ liệu thành công", envelope.message)
        assertNotNull(envelope.data)
        assertEquals("anh_nguyen", envelope.data?.get("username"))
        assertTrue(envelope.errors == null || envelope.errors!!.isJsonNull)
        assertEquals(200, envelope.statusCode)
    }

    @Test
    fun testParseValidationErrorsEnvelope() {
        val json = """
            {
                "success": false,
                "message": "Validation failed",
                "data": null,
                "errors": {
                    "validation": [
                        "username should not be empty",
                        "password must be at least 6 characters long"
                    ],
                    "error": "Bad Request",
                    "statusCode": 400
                },
                "statusCode": 400
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponseDto<Any>>() {}.type
        val envelope: ApiResponseDto<Any> = gson.fromJson(json, type)

        assertFalse(envelope.success)
        val validationErrors = envelope.extractValidationErrors()
        assertNotNull(validationErrors)
        assertEquals(2, validationErrors?.size)
        assertEquals("username should not be empty", validationErrors?.get(0))
        assertEquals("password must be at least 6 characters long", validationErrors?.get(1))

        val fullError = envelope.extractErrorMessage()
        assertTrue(fullError.contains("username should not be empty"))
        assertTrue(fullError.contains("password must be at least 6 characters long"))
    }

    @Test
    fun testParseSimpleErrorEnvelope() {
        val json = """
            {
                "success": false,
                "message": "Invalid credentials",
                "data": null,
                "errors": null,
                "statusCode": 401
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponseDto<Any>>() {}.type
        val envelope: ApiResponseDto<Any> = gson.fromJson(json, type)

        assertFalse(envelope.success)
        assertEquals("Invalid credentials", envelope.extractErrorMessage())
    }
}
