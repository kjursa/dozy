package com.dozy.features.place.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.place.CreatedPlaceResponse
import com.dozy.api.contracts.v1.place.PlacesResponse
import com.dozy.api.contracts.v1.place.PlacesSaveError
import com.dozy.api.contracts.v1.place.SavePlaceRequest
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.application.PlaceService
import com.dozy.features.place.domain.model.ParseResult
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.plugins.ApiException
import com.dozy.plugins.BusinessApiException
import com.dozy.plugins.idempotencyKey
import com.dozy.plugins.ownerId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.placeV1Routes(service: PlaceService) {
    route("/places") {
        get {
            val places = service.list(call.ownerId())
            call.respond(PlacesResponse(places.map { it.toResponse() }))
        }
        post {
            val ownerId = call.ownerId()
            val requestKey = call.idempotencyKey()
            call.respondToSave(
                service.create(
                    ownerId,
                    call.receive<SavePlaceRequest>().toCommand(),
                    requestKey
                )
            )
        }
        put("/{id}") {
            val ownerId = call.ownerId()
            val id = requirePlaceId(call.parameters["id"])
            call.respondToSave(
                service.save(ownerId, id, call.receive<SavePlaceRequest>().toCommand())
            )
        }
    }
}

private fun requirePlaceId(path: String?): PlaceId =
    PlaceId.parse(path) ?: throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_REQUEST)

private fun SavePlace.Error.toApiException(): BusinessApiException {
    val error = when (this) {
        SavePlace.Error.Name.Required -> PlacesSaveError.NameRequired
        SavePlace.Error.Name.TooLong -> PlacesSaveError.NameTooLong
        SavePlace.Error.Description.Required -> PlacesSaveError.DescriptionRequired
        SavePlace.Error.Description.TooLong -> PlacesSaveError.DescriptionTooLong
        SavePlace.Error.Inhabitants.TooLong -> PlacesSaveError.InhabitantsTooLong
    }
    return BusinessApiException(HttpStatusCode.UnprocessableEntity, error)
}

private fun SavePlaceRequest.toCommand(): SavePlace =
    when (val result = SavePlace.parse(name, description, inhabitants)) {
        is ParseResult.Valid -> result.value
        is ParseResult.Invalid -> throw result.error.toApiException()
    }

private suspend fun ApplicationCall.respondToSave(result: SavePlaceResult) {
    when (result) {
        is SavePlaceResult.Created -> respond(
            HttpStatusCode.Created,
            CreatedPlaceResponse(result.id.value)
        )
        SavePlaceResult.Updated -> respond(HttpStatusCode.NoContent)
        SavePlaceResult.NotFound -> throw BusinessApiException(
            HttpStatusCode.NotFound,
            PlacesSaveError.NotFound
        )
        SavePlaceResult.Forbidden -> throw BusinessApiException(
            HttpStatusCode.Forbidden,
            PlacesSaveError.Forbidden
        )
        SavePlaceResult.IdempotencyConflict -> throw BusinessApiException(
            HttpStatusCode.Conflict,
            PlacesSaveError.IdempotencyConflict
        )
        SavePlaceResult.Conflict -> throw BusinessApiException(
            HttpStatusCode.Conflict,
            PlacesSaveError.Conflict
        )
    }
}
