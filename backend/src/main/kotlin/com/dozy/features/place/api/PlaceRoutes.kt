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
                is ParseResult.Invalid -> throw ApiException(HttpStatusCode.UnprocessableEntity, "INVALID_REQUEST")
            }
            call.respond(service.save(installation, id, command).status())
        }
    }
}

private fun requirePlaceId(path: String?, request: SavePlaceRequest): Place.Id {
    if (path.isNullOrBlank() || path != request.id || request.ownership != "CUSTOM" || !request.editable) {
        throw ApiException(HttpStatusCode.BadRequest, "INVALID_REQUEST")
    }
    return when (val result = Place.Id.parse(path)) {
        is ParseResult.Valid -> result.value
        is ParseResult.Invalid -> throw ApiException(HttpStatusCode.BadRequest, "INVALID_REQUEST")
    }
}

private fun SavePlaceResult.status(): HttpStatusCode = when (this) {
    SavePlaceResult.Created -> HttpStatusCode.Created
    SavePlaceResult.Updated -> HttpStatusCode.NoContent
    SavePlaceResult.Forbidden -> throw ApiException(HttpStatusCode.Forbidden, "FORBIDDEN")
    SavePlaceResult.Conflict -> throw ApiException(HttpStatusCode.Conflict, "CONFLICT")
}
