package com.nihonix.mobile.feature.topics.domain.model

data class LocalizedText(
    val vi: String = "",
    val en: String = "",
    val jp: String = ""
) {
    fun getByLanguage(language: String?): String {
        return when (language?.lowercase()) {
            "vi" -> vi.ifBlank { en.ifBlank { jp } }
            "jp", "ja" -> jp.ifBlank { en.ifBlank { vi } }
            else -> en.ifBlank { vi.ifBlank { jp } }
        }
    }
}
