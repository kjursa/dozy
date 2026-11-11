package com.dozy.mobile.feature.character.data.api

import kotlinx.serialization.Serializable

@Serializable
data class AvatarRequestDto(
    val type: String,
    val personality: String,
    val appearance: String,
)

@Serializable
data class AvatarDto(
    val id: String,
    val url: String,
)

@Serializable
data class AvatarJobDto(
    val jobId: String,
    val status: String,
    val asset: AvatarDto? = null,
    val error: String? = null,
)
