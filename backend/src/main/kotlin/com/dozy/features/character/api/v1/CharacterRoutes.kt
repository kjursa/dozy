package com.dozy.features.character.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.CharacterType
import com.dozy.api.contracts.v1.character.CharacterField
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import com.dozy.features.character.application.CharacterService
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.features.installation.api.installationId
import com.dozy.plugins.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.application
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.characterV1Routes() {
    val service: CharacterService by application.dependencies
    route("/characters") {
        get { call.respond(service.list(call.installationId()).map { it.toDto() }) }
        put("/{id}") {
            val installation = call.installationId()
            val id = requireCharacterId(call.parameters["id"])
            val request = call.receive<SaveCharacterRequest>()
            val command = when (val parsed = SaveCharacter.parse(
                request.name, request.personality, request.appearance, request.type,
                request.avatar?.let { Character.Avatar(it.id, it.url) },
            )) {
                is SaveCharacter.Result.Valid -> parsed.command
                is SaveCharacter.Result.Invalid -> throw parsed.toApiException()
            }
            val status = when (service.save(installation, id, command)) {
                SaveCharacterResult.Created -> HttpStatusCode.Created
                SaveCharacterResult.Updated -> HttpStatusCode.NoContent
                SaveCharacterResult.Conflict -> throw ApiException(HttpStatusCode.Conflict, ApiErrorCode.CONFLICT)
                SaveCharacterResult.ReferenceUnavailable -> throw ApiException(HttpStatusCode.UnprocessableEntity, ApiErrorCode.REFERENCE_UNAVAILABLE)
            }
            call.respond(status)
        }
        delete("/{id}") {
            val installation = call.installationId()
            if (!service.delete(installation, requireCharacterId(call.parameters["id"]))) {
                throw ApiException(HttpStatusCode.NotFound, ApiErrorCode.CHARACTER_NOT_FOUND)
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun requireCharacterId(raw: String?): Character.Id {
    val uuid = raw?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (uuid == null || !uuid.toString().equals(raw, ignoreCase = true) || uuid.version() != 7 || uuid.variant() != 2) {
        throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_REQUEST)
    }
    return Character.Id(uuid.toString())
}

private fun SaveCharacter.Result.Invalid.toApiException(): ApiException {
    val field = CharacterField.valueOf(field.name)
    val reason = ValidationReason.valueOf(reason.name)
    check(reason in field.reasons)
    return ApiException(HttpStatusCode.UnprocessableEntity, ApiErrorCode.VALIDATION_FAILED, field.value, reason.value)
}

private fun Character.toDto() = CharacterDto(
    id.value, name, personality, appearance, CharacterType.valueOf(type.name).name, avatar?.let { AvatarDto(it.id, it.url) },
)
