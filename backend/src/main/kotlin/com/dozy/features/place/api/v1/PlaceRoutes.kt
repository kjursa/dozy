package com.dozy.features.place.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.place.CreatedPlaceResponse
import com.dozy.api.contracts.v1.place.PlaceField
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.PlacesSaveError
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.application.PlaceService
import com.dozy.features.place.domain.model.ParseResult
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.plugins.badRequest
import com.dozy.plugins.collectionRevalidation
import com.dozy.plugins.conflict
import com.dozy.plugins.created
import com.dozy.plugins.forbidden
import com.dozy.plugins.idempotencyKey
import com.dozy.plugins.noContent
import com.dozy.plugins.notFound
import com.dozy.plugins.ownerId
import com.dozy.plugins.respondVersionedCollection
import com.dozy.plugins.unprocessableEntity
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.security.MessageDigest
import java.util.HexFormat

fun Route.placeV1Routes(
    service: PlaceService,
    assetBaseUrl: String,
) {
    val assetVersion =
        HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(assetBaseUrl.trimEnd('/').encodeToByteArray()),
        )
    staticResources("/place-icons", "fixtures/place-icons")
    route("/places") {
        get {
            val isCurrent = call.collectionRevalidation()
            val (version, places) = service.list(call.ownerId()) { isCurrent("$it.$assetVersion") }
            val response = places?.let { PlacesResponse(it.map { place -> place.toResponse(assetBaseUrl) }) }
            call.respondVersionedCollection("$version.$assetVersion", response)
        }
        post {
            val ownerId = call.ownerId()
            val requestKey =
                call.idempotencyKey {
                    return@post call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
                }
            val command =
                call.receive<SavePlaceRequest>().parseCommand { error ->
                    return@post call.unprocessableEntity(error)
                }
            call.respondToSave(
                service.create(
                    ownerId,
                    command,
                    requestKey,
                ),
                assetBaseUrl,
            )
        }
        put("/{id}") {
            val ownerId = call.ownerId()
            val id =
                PlaceId.parse(call.parameters["id"])
                    ?: return@put call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
            val command =
                call.receive<SavePlaceRequest>().parseCommand { error ->
                    return@put call.unprocessableEntity(error)
                }
            call.respondToSave(
                service.save(ownerId, id, command),
                assetBaseUrl,
            )
        }
    }
}

private fun SavePlace.Error.toApiError(): PlacesSaveError {
    val (field, reason) =
        when (this) {
            SavePlace.Error.Name.Required -> PlaceField.NAME to ValidationReason.REQUIRED
            SavePlace.Error.Name.TooLong -> PlaceField.NAME to ValidationReason.TOO_LONG
            SavePlace.Error.Description.Required -> PlaceField.DESCRIPTION to ValidationReason.REQUIRED
            SavePlace.Error.Description.TooLong -> PlaceField.DESCRIPTION to ValidationReason.TOO_LONG
            SavePlace.Error.Inhabitants.TooLong -> PlaceField.INHABITANTS to ValidationReason.TOO_LONG
        }
    return PlacesSaveError.invalidField(field, reason)
}

private inline fun SavePlaceRequest.parseCommand(onInvalid: (PlacesSaveError) -> Nothing): SavePlace =
    when (val result = SavePlace.parse(name, description, inhabitants)) {
        is ParseResult.Valid -> result.value
        is ParseResult.Invalid -> onInvalid(result.error.toApiError())
    }

private suspend fun ApplicationCall.respondToSave(
    result: SavePlaceResult,
    assetBaseUrl: String,
) {
    when (result) {
        is SavePlaceResult.Created ->
            created(CreatedPlaceResponse(result.id.value, Place.Thumbnail.COTTAGE.imageUrl(assetBaseUrl)))
        SavePlaceResult.Updated -> noContent()
        SavePlaceResult.NotFound ->
            notFound(PlacesSaveError.NotFound)
        SavePlaceResult.Forbidden ->
            forbidden(PlacesSaveError.Forbidden)
        SavePlaceResult.IdempotencyConflict ->
            conflict(PlacesSaveError.IdempotencyConflict)
        SavePlaceResult.Conflict ->
            conflict(PlacesSaveError.Conflict)
    }
}
