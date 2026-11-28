package com.dozy.features.character.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
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
import com.dozy.plugins.ApiException
import com.dozy.plugins.BusinessApiException
import com.dozy.plugins.collectionRevalidation
import com.dozy.plugins.conflict
import com.dozy.plugins.created
import com.dozy.plugins.idempotencyKey
import com.dozy.plugins.noContent
import com.dozy.plugins.notFound
import com.dozy.plugins.ownerId
import com.dozy.plugins.respondVersionedCollection
import com.dozy.plugins.unprocessableEntity
import io.ktor.http.HttpStatusCode
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
            val requestKey = call.idempotencyKey()
            call.respondToSave(
                service.create(
                    ownerId,
                    call.receive<SaveCharacterRequest>().toCommand(),
                    requestKey,
                ),
            )
        }
        put("/{id}") {
            val ownerId = call.ownerId()
            val id = requireCharacterId(call.parameters["id"])
            call.respondToSave(
                service.save(ownerId, id, call.receive<SaveCharacterRequest>().toCommand()),
            )
        }
        delete("/{id}") {
            val ownerId = call.ownerId()
            if (!service.delete(ownerId, requireCharacterId(call.parameters["id"]))) {
                call.notFound(CharactersDeleteError.NotFound)
                return@delete
            }
            call.noContent()
        }
    }
}

private fun SaveCharacterRequest.toCommand(): SaveCharacter =
    when (
        val parsed =
            SaveCharacter.parse(
                name,
                personality,
                appearance,
                type,
                avatar?.let {
                    val id =
                        AssetId.parse(it.id) ?: throw BusinessApiException(
                            HttpStatusCode.UnprocessableEntity,
                            CharactersSaveError.ReferenceUnavailable,
                        )
                    Character.Avatar(id, it.url)
                },
            )
    ) {
        is SaveCharacter.Result.Valid -> parsed.command
        is SaveCharacter.Result.Invalid -> throw parsed.toApiException()
    }

private suspend fun ApplicationCall.respondToSave(result: SaveCharacterResult) {
    when (result) {
        is SaveCharacterResult.Created -> created(CreatedCharacterResponse(result.id.value))
        SaveCharacterResult.Updated -> noContent()
        SaveCharacterResult.NotFound -> notFound(CharactersSaveError.NotFound)
        SaveCharacterResult.IdempotencyConflict -> conflict(CharactersSaveError.IdempotencyConflict)
        SaveCharacterResult.Conflict -> conflict(CharactersSaveError.Conflict)
        SaveCharacterResult.ReferenceUnavailable -> unprocessableEntity(CharactersSaveError.ReferenceUnavailable)
    }
}

private fun requireCharacterId(raw: String?): CharacterId =
    CharacterId.parse(raw) ?: throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_REQUEST)

private fun SaveCharacter.Result.Invalid.toApiException(): BusinessApiException {
    val field = CharacterField.valueOf(field.name)
    val reason = ValidationReason.valueOf(reason.name)
    check(reason in field.reasons)
    return BusinessApiException(
        HttpStatusCode.UnprocessableEntity,
        CharactersSaveError.Validation(field, reason),
    )
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
