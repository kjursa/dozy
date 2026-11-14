package com.dozy.features.place.api

import com.dozy.features.installation.api.installationId
import com.dozy.features.place.application.PlaceService
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.ParseResult
import com.dozy.features.place.domain.model.SavePlace
import com.dozy.features.place.domain.model.SavePlaceResult
import com.dozy.plugins.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.placeRoutes(service: PlaceService) {
    route("/places") {
        get {
            val places = service.list(call.installationId())
            call.respond(PlacesResponse(places.map { it.toResponse() }))
        }
        put("/{id}") {
            val installation = call.installationId()
            val request = call.receive<SavePlaceRequest>()
            val id = requirePlaceId(call.parameters["id"], request)
            val command = when (val result = SavePlace.parse(request.name, request.description, request.inhabitants)) {
                is ParseResult.Valid -> result.value
                is ParseResult.Invalid -> throw result.error.toApiException()
            }
            call.respond(service.save(installation, id, command).status())
        }
    }
}

private fun requirePlaceId(path: String?, request: SavePlaceRequest): Place.Id {
    if (path.isNullOrBlank() || path != request.id || request.ownership != "CUSTOM" || !request.editable) {
        throw ApiException(HttpStatusCode.BadRequest, "INVALID_REQUEST")
    }
    return Place.Id(path)
}

private fun SavePlace.Error.toApiException(): ApiException = when (this) {
    SavePlace.Error.Name.Required -> fieldError("name", "REQUIRED")
    is SavePlace.Error.Name.TooLong -> fieldError("name", "TOO_LONG", maxLength)
    SavePlace.Error.Description.Required -> fieldError("description", "REQUIRED")
    is SavePlace.Error.Description.TooLong -> fieldError("description", "TOO_LONG", maxLength)
    is SavePlace.Error.Inhabitants.TooLong -> fieldError("inhabitants", "TOO_LONG", maxLength)
}

private fun fieldError(field: String, reason: String, maxLength: Int? = null) =
    ApiException(HttpStatusCode.UnprocessableEntity, "INVALID_REQUEST", field, reason, maxLength)

private fun SavePlaceResult.status(): HttpStatusCode = when (this) {
    SavePlaceResult.Created -> HttpStatusCode.Created
    SavePlaceResult.Updated -> HttpStatusCode.NoContent
    SavePlaceResult.Forbidden -> throw ApiException(HttpStatusCode.Forbidden, "FORBIDDEN")
    SavePlaceResult.Conflict -> throw ApiException(HttpStatusCode.Conflict, "CONFLICT")
}
