package com.dozy.features.story.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.CharacterType
import com.dozy.api.contracts.v1.story.GenerationField
import com.dozy.api.contracts.v1.story.GenerationMode
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusDto
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.api.contracts.v1.story.StoryLength
import com.dozy.api.contracts.v1.story.StoryPageDto
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.api.contracts.v1.story.StorySyncDto
import com.dozy.features.story.data.toDomain
import com.dozy.features.story.data.toDto
import com.dozy.features.story.domain.model.GenerationStatus
import com.dozy.features.story.domain.model.SavedStory
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.StoryPage
import com.dozy.features.story.domain.model.StorySummary
import com.dozy.features.story.domain.model.StorySync
import com.dozy.plugins.ApiException
import com.dozy.plugins.BusinessApiException
import io.ktor.http.HttpStatusCode
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
internal fun JsonObject.command(): StartGeneration {
    val dto = try { Json.decodeFromJsonElement<GenerationRequestDto>(this) } catch (_: SerializationException) { invalidRequest() }
    if (GenerationMode.entries.none { it.name == dto.mode } || StoryLength.entries.none { it.name == dto.length }) invalidRequest()
    if (dto.mode == GenerationMode.AUTOMATIC.name && keys.any { it in setOf("place", "idea", "ending") }) invalidRequest()
    dto.cast.forEach {
        if (it.name.isBlank() || it.personality.isBlank() || it.appearance.isBlank() || CharacterType.entries.none { type -> type.name == it.type }) invalidRequest()
        if (it.avatar?.url?.isBlank() == true) invalidRequest()
    }
    dto.place?.let {
        if (it.id.isBlank() || it.name.isBlank() || it.description.isBlank() || it.origin !in setOf("USER", "SYSTEM")) invalidRequest()
    }
    return when (val parsed = StartGeneration.parse(dto.toDomain() ?: invalidRequest())) {
        is StartGeneration.Result.Valid -> parsed.command
        is StartGeneration.Result.Invalid -> {
            val field = GenerationField.valueOf(parsed.field.name)
            val reason = ValidationReason.valueOf(parsed.reason.name)
            check(reason in field.reasons)
            throw BusinessApiException(HttpStatusCode.UnprocessableEntity, GenerationStartError.Validation(field, reason))
        }
    }
}
internal fun GenerationStatus.toDto() = GenerationStatusDto(job.id.value, job.stage.name, job.storyId?.value, job.failure?.name, story?.toDto())
internal fun SavedStory.toDto() = StoryDto(id.value, title, text, coverUrl, configuration.toDto(), createdAt, jobId.value)

internal fun StoryPage.toDto() =
    StoryPageDto(
        stories.map { it.toDto() },
        nextCursor?.value,
    )

internal fun StorySync.toDto() = StorySyncDto(upserts.map { it.toDto() }, deletedIds.map { it.value }, nextRevision)

private fun StorySummary.toDto() = StorySummaryDto(id.value, title, coverUrl, readingMinutes, createdAt)
