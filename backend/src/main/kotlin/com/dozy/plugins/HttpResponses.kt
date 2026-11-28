package com.dozy.plugins

import com.dozy.api.contracts.v1.ApiBusinessError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

suspend fun ApplicationCall.ok(body: Any) = respond(HttpStatusCode.OK, body)

suspend fun ApplicationCall.created(body: Any) = respond(HttpStatusCode.Created, body)

suspend fun ApplicationCall.accepted(body: Any) = respond(HttpStatusCode.Accepted, body)

suspend fun ApplicationCall.noContent() = respond(HttpStatusCode.NoContent)

suspend fun ApplicationCall.badRequest(body: Any) = respond(HttpStatusCode.BadRequest, body)

suspend fun ApplicationCall.unauthorized(body: Any) = respond(HttpStatusCode.Unauthorized, body)

suspend fun ApplicationCall.forbidden(body: Any) = respond(HttpStatusCode.Forbidden, body)

suspend fun ApplicationCall.notFound(body: Any) = respond(HttpStatusCode.NotFound, body)

suspend fun ApplicationCall.notFound() = respond(HttpStatusCode.NotFound)

suspend fun ApplicationCall.conflict(body: Any) = respond(HttpStatusCode.Conflict, body)

suspend fun ApplicationCall.unprocessableEntity(body: Any) = respond(HttpStatusCode.UnprocessableEntity, body)

suspend fun ApplicationCall.tooManyRequests(body: Any) = respond(HttpStatusCode.TooManyRequests, body)

suspend fun ApplicationCall.serviceUnavailable(body: Any) = respond(HttpStatusCode.ServiceUnavailable, body)

suspend fun ApplicationCall.badRequest(error: ApiBusinessError) = respondApiError(HttpStatusCode.BadRequest, error)

suspend fun ApplicationCall.unauthorized(error: ApiBusinessError) = respondApiError(HttpStatusCode.Unauthorized, error)

suspend fun ApplicationCall.forbidden(error: ApiBusinessError) = respondApiError(HttpStatusCode.Forbidden, error)

suspend fun ApplicationCall.notFound(error: ApiBusinessError) = respondApiError(HttpStatusCode.NotFound, error)

suspend fun ApplicationCall.conflict(error: ApiBusinessError) = respondApiError(HttpStatusCode.Conflict, error)

suspend fun ApplicationCall.unprocessableEntity(error: ApiBusinessError) = respondApiError(HttpStatusCode.UnprocessableEntity, error)

suspend fun ApplicationCall.tooManyRequests(error: ApiBusinessError) = respondApiError(HttpStatusCode.TooManyRequests, error)
