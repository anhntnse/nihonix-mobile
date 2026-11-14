package com.nihonix.mobile.feature.topics.domain.model

import com.google.gson.annotations.SerializedName

data class BundledTopic(val id: String, val slug: String,
    @SerializedName("level_code") val levelCode: String, val title: LocalizedText)
