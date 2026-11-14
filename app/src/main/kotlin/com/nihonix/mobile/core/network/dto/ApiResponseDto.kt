package com.nihonix.mobile.core.network.dto

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Common standard API response envelope matching backend ResponseInterceptor.
 */
data class ApiResponseDto<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("errors")
    val errors: JsonElement? = null,

    @SerializedName("statusCode")
    val statusCode: Int? = null
) {
    /**
     * Extracts readable validation errors if present in backend error envelope.
     */
    fun extractValidationErrors(): List<String>? {
        if (errors == null || errors.isJsonNull || !errors.isJsonObject) return null
        val obj = errors.asJsonObject
        if (obj.has("validation") && obj.get("validation").isJsonArray) {
            val arr = obj.getAsJsonArray("validation")
            return arr.mapNotNull { it.asString }
        }
        return null
    }

    /**
     * Extracts the best error message.
     */
    fun extractErrorMessage(): String {
        val validation = extractValidationErrors()
        if (!validation.isNullOrEmpty()) {
            return validation.joinToString("\n")
        }
        if (!message.isNullOrBlank()) {
            return message
        }
        if (errors != null && !errors.isJsonNull && errors.isJsonObject) {
            val obj = errors.asJsonObject
            if (obj.has("message") && obj.get("message").isJsonPrimitive) {
                return obj.get("message").asString
            }
        }
        return "Unknown error occurred"
    }
}
