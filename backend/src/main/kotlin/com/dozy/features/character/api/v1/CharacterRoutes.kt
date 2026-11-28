package com.dozy.features.character.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.character.CharacterDto
import com.dozy.api.contracts.v1.character.CharacterField
import com.dozy.api.contracts.v1.character.CharacterType
import com.dozy.api.contracts.v1.character.CharactersDeleteError
import com.dozy.api.contracts.v1.character.CharactersSaveError
import com.dozy.api.contracts.v1.character.CreatedCharacterResponse
import com.dozy.api.contracts.v1.character.SaveCharacterRequest
import com.dozy.domain.id.AssetId
import com.dozy.domain.id.CharacterId
import com.dozy.features.character.application.CharacterService
import com.dozy.features.character.domain.model.Character
import com.dozy.features.character.domain.model.SaveCharacter
import com.dozy.features.character.domain.model.SaveCharacterResult
import com.dozy.plugins.badRequest
import com.dozy.plugins.collectionRevalidation
import com.dozy.plugins.conflict
import com.dozy.plugins.created
import com.dozy.plugins.idempotencyKey
import com.dozy.plugins.noContent
import com.dozy.plugins.notFound
import com.dozy.plugins.ownerId
import com.dozy.plugins.respondVersionedCollection
import com.dozy.plugins.unprocessableEntity
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.characterV1Routes() {
    val service: CharacterService by application.dependencies
    route("/characters") {
        get {
            val (version, characters) = service.list(call.ownerId(), call.collectionRevalidation())
            call.respondVersionedCollection(version, characters?.sortedBy { it.id.value }?.map { it.toDto() })
        }
        post {
            val ownerId = call.ownerId()
            val requestKey =
                call.idempotencyKey {
                    return@post call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
                }
            val command =
                call.receive<SaveCharacterRequest>().parseCommand { error ->
                    return@post call.unprocessableEntity(error)
                }
            call.respondToSave(
                service.create(
                    ownerId,
                    command,
                    requestKey,
                ),
            )
        }
        put("/{id}") {
            val ownerId = call.ownerId()
            val id =
                CharacterId.parse(call.parameters["id"])
                    ?: return@put call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
            val command =
                call.receive<SaveCharacterRequest>().parseCommand { error ->
                    return@put call.unprocessableEntity(error)
                }
            call.respondToSave(
                service.save(ownerId, id, command),
            )
        }
        delete("/{id}") {
            val ownerId = call.ownerId()
            val id =
                CharacterId.parse(call.parameters["id"])
                    ?: return@delete call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
            if (!service.delete(ownerId, id)) {
                return@delete call.notFound(CharactersDeleteError.NotFound)
            }
            call.noContent()
        }
    }
}

private inline fun SaveCharacterRequest.parseCommand(onInvalid: (CharactersSaveError) -> Nothing): SaveCharacter {
    val avatar =
        avatar?.let {
            val id = AssetId.parse(it.id) ?: onInvalid(CharactersSaveError.ReferenceUnavailable)
            Character.Avatar(id, it.url)
        }
    return when (val parsed = SaveCharacter.parse(name, personality, appearance, type, avatar)) {
        is SaveCharacter.Result.Valid -> parsed.command
        is SaveCharacter.Result.Invalid -> onInvalid(parsed.toApiError())
    }
}

private suspend fun ApplicationCall.respondToSave(result: SaveCharacterResult) {
    when (result) {
        is SaveCharacterResult.Created ->
            created(CreatedCharacterResponse(result.id.value))
        SaveCharacterResult.Updated -> noContent()
        SaveCharacterResult.NotFound ->
            notFound(CharactersSaveError.NotFound)
        SaveCharacterResult.IdempotencyConflict ->
            conflict(CharactersSaveError.IdempotencyConflict)
        SaveCharacterResult.Conflict ->
            conflict(CharactersSaveError.Conflict)
        SaveCharacterResult.ReferenceUnavailable ->
            unprocessableEntity(CharactersSaveError.ReferenceUnavailable)
    }
}

private fun SaveCharacter.Result.Invalid.toApiError(): CharactersSaveError.Validation {
    val field = CharacterField.valueOf(field.name)
    val reason = ValidationReason.valueOf(reason.name)
    check(reason in field.reasons)
    return CharactersSaveError.Validation(field, reason)
}

private fun Character.toDto() =
    CharacterDto(
        id.value,
        name,
        personality,
        appearance,
        CharacterType.valueOf(type.name).name,
        avatar?.let {
            AvatarDto(it.id.value, it.url)
        },
    )
