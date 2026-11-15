package com.dozy.features.story.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.StoryListField
import com.dozy.features.installation.api.installationId
import com.dozy.features.story.application.StoryService
import com.dozy.features.story.domain.model.Submission
import com.dozy.plugins.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.JsonObject

fun Route.storyV1Routes(service: StoryService) {
    post("/story-generations") {
        val installation = call.installationId()
        val key = requireUuid(call.request.headers.getAll("Idempotency-Key")?.singleOrNull(), 4)
        val body = call.receive<JsonObject>()
        val command = body.command()
        when (val result = service.start(installation, key, canonicalJson(body), command)) {
            is Submission.Accepted -> call.respond(HttpStatusCode.Accepted, GenerationAcceptedDto(result.job.id, result.job.stage.name))
            Submission.IdempotencyConflict -> throw ApiException(HttpStatusCode.Conflict, ApiErrorCode.IDEMPOTENCY_CONFLICT)
            Submission.ActiveJobExists -> throw ApiException(HttpStatusCode.Conflict, ApiErrorCode.ACTIVE_JOB_EXISTS)
            Submission.ReferenceUnavailable -> throw ApiException(HttpStatusCode.UnprocessableEntity, ApiErrorCode.REFERENCE_UNAVAILABLE)
        }
    }
    get("/story-generations/{id}") {
        val job = service.status(call.installationId(), call.parameters["id"].orEmpty())
            ?: throw ApiException(HttpStatusCode.NotFound, ApiErrorCode.JOB_NOT_FOUND)
        call.respond(job.toDto())
    }
    get("/stories") {
        val installation = call.installationId()
        val limits = call.request.queryParameters.getAll("limit")
        val limit = if (limits == null) 100 else limits.singleOrNull()?.takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
        if (limit == null || limit !in 1..100) throw ApiException(HttpStatusCode.UnprocessableEntity, ApiErrorCode.VALIDATION_FAILED, StoryListField.LIMIT.value, ValidationReason.INVALID_VALUE.value)
        val cursors = call.request.queryParameters.getAll("cursor")
        if (cursors != null && (cursors.size != 1 || cursors.single().isBlank())) throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_CURSOR)
        val page = service.list(installation, limit, cursors?.single())
            ?: throw ApiException(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_CURSOR)
        call.respond(page.toDto())
    }
    get("/stories/{id}") {
        val story = service.read(call.installationId(), call.parameters["id"].orEmpty())
            ?: throw ApiException(HttpStatusCode.NotFound, ApiErrorCode.STORY_NOT_FOUND)
        call.respond(story.toDto())
    }
}
