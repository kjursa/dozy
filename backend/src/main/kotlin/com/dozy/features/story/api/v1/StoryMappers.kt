package com.dozy.features.story.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.CharacterType
import com.dozy.api.contracts.v1.story.GenerationField
import com.dozy.api.contracts.v1.story.GenerationMode
import com.dozy.api.contracts.v1.story.StoryLength
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.features.story.data.toDomain
import com.dozy.features.story.data.toDto
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.SavedStory
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.plugins.ApiException
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

internal fun canonicalJson(value: JsonElement): String = when (value) {
    is JsonObject -> JsonObject(value.toSortedMap().mapValues { Json.parseToJsonElement(canonicalJson(it.value)) }).toString()
    is JsonArray -> JsonArray(value.map { Json.parseToJsonElement(canonicalJson(it)) }).toString()
    else -> value.toString()
}
internal fun invalidRequest(): Nothing = throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_REQUEST)
internal fun requireUuid(raw: String?, version: Int): String {
    val uuid = raw?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (uuid == null || uuid.version() != version || uuid.variant() != 2 || !uuid.toString().equals(raw, true)) invalidRequest()
    return uuid.toString()
}
internal fun JsonObject.command(): StartGeneration {
    val dto = try { Json.decodeFromJsonElement<GenerationRequestDto>(this) } catch (_: SerializationException) { invalidRequest() }
    if (GenerationMode.entries.none { it.name == dto.mode } || StoryLength.entries.none { it.name == dto.length }) invalidRequest()
    if (dto.mode == GenerationMode.AUTOMATIC.name && keys.any { it in setOf("place", "idea", "ending") }) invalidRequest()
    dto.cast.forEach {
        requireUuid(it.id, 7)
        if (it.name.isBlank() || it.personality.isBlank() || it.appearance.isBlank() || CharacterType.entries.none { type -> type.name == it.type }) invalidRequest()
        it.avatar?.let { avatar -> requireUuid(avatar.id, 7); if (avatar.url.isBlank()) invalidRequest() }
    }
    dto.place?.let {
        if (it.id.isBlank() || it.name.isBlank() || it.description.isBlank() || it.origin !in setOf("USER", "SYSTEM")) invalidRequest()
    }
    return when (val parsed = StartGeneration.parse(dto.toDomain())) {
        is StartGeneration.Result.Valid -> parsed.command
        is StartGeneration.Result.Invalid -> {
            val field = GenerationField.valueOf(parsed.field.name)
            val reason = ValidationReason.valueOf(parsed.reason.name)
            check(reason in field.reasons)
            throw ApiException(HttpStatusCode.UnprocessableEntity, ApiErrorCode.VALIDATION_FAILED, field.value, reason.value)
        }
    }
}
internal fun Job.toDto() = GenerationStatusDto(id, stage.name, storyId, failure?.name)
internal fun SavedStory.toDto() = StoryDto(id, title, text, coverUrl, configuration.toDto(), createdAt)
