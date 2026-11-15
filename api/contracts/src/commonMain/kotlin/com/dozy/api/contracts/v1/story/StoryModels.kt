package com.dozy.api.contracts.v1.story

import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.api.contracts.v1.ValidationReason
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class GenerationRequestDto(
    val cast: List<CharacterDto>,
    val mode: String,
    val length: String,
    val place: PlaceResponse? = null,
    val idea: String? = null,
    val ending: String? = null,
)

/** AUTOMATIC never submits draft-only manual fields, even with encodeDefaults enabled. */
fun GenerationRequestDto.toRequestBody(): JsonObject {
    val fields = Json.encodeToJsonElement(this).jsonObject
    return JsonObject(if (mode == GenerationMode.AUTOMATIC.name) fields.filterKeys { it in setOf("cast", "mode", "length") } else fields)
}

@Serializable
data class GenerationAcceptedDto(val jobId: String, val status: String)

@Serializable
data class GenerationStatusDto(val jobId: String, val status: String, val storyId: String? = null, val errorCode: String? = null)

@Serializable
data class StoryDto(
    val id: String,
    val title: String,
    val text: String,
    val coverUrl: String,
    val configuration: GenerationRequestDto,
    val createdAt: Long = 0,
)

enum class GenerationMode { MANUAL, AUTOMATIC }
enum class StoryLength { SHORT, LONG }
enum class GenerationStage { QUEUED, CREATING_TEXT, CREATING_COVER, READY, FAILED }
enum class GenerationFailure { CONTENT_REJECTED, GENERATION_FAILED }
enum class GenerationField(val reasons: Set<ValidationReason>) {
    CAST(setOf(ValidationReason.REQUIRED, ValidationReason.INVALID_VALUE)),
    IDEA(setOf(ValidationReason.TOO_LONG)),
    ENDING(setOf(ValidationReason.TOO_LONG));

    val value: String get() = name.lowercase()

    companion object {
        fun fromValue(value: String): GenerationField? = entries.firstOrNull { it.value == value }
    }
}
