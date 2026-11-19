package com.dozy.features.story.api.v1

import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusError
import com.dozy.api.contracts.v1.story.StoryListError
import com.dozy.api.contracts.v1.story.StoryListField
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.domain.id.JobId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.application.StoryService
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.Submission
import com.dozy.plugins.BusinessApiException
import com.dozy.plugins.idempotencyKey
import com.dozy.plugins.ownerId
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
        val ownerId = call.ownerId()
        val key = call.idempotencyKey() ?: invalidRequest()
        val body = call.receive<JsonObject>()
        val command = body.command()
        when (val result = service.start(ownerId, key, canonicalJson(body), command)) {
            is Submission.Accepted -> call.respond(HttpStatusCode.Accepted, GenerationAcceptedDto(result.job.id.value, result.job.stage.name))
            Submission.IdempotencyConflict -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationStartError.IdempotencyConflict)
            Submission.ActiveJobExists -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationStartError.ActiveJobExists)
            Submission.ReferenceUnavailable -> throw BusinessApiException(HttpStatusCode.UnprocessableEntity, GenerationStartError.ReferenceUnavailable)
        }
    }
    post("/story-generations/{id}/retry") {
        val ownerId = call.ownerId()
        val key = call.idempotencyKey() ?: invalidRequest()
        val id = JobId.parse(call.parameters["id"])
            ?: throw BusinessApiException(HttpStatusCode.NotFound, GenerationRetryError.JobNotFound)
        when (val result = service.retry(ownerId, id, key)) {
            is RetrySubmission.Accepted -> call.respond(HttpStatusCode.Accepted, GenerationAcceptedDto(result.job.id.value, result.job.stage.name))
            RetrySubmission.JobNotFound -> throw BusinessApiException(HttpStatusCode.NotFound, GenerationRetryError.JobNotFound)
            RetrySubmission.JobNotFailed -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationRetryError.JobNotFailed)
            RetrySubmission.ActiveJobExists -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationRetryError.ActiveJobExists)
            RetrySubmission.IdempotencyConflict -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationRetryError.IdempotencyConflict)
        }
    }
    get("/story-generations/{id}") {
        val status = JobId.parse(call.parameters["id"])?.let { service.status(call.ownerId(), it) }
            ?: throw BusinessApiException(HttpStatusCode.NotFound, GenerationStatusError.NotFound)
        call.respond(status.toDto())
    }
    get("/stories") {
        val ownerId = call.ownerId()
        val limits = call.request.queryParameters.getAll("limit")
        val limit = if (limits == null) 100 else limits.singleOrNull()?.takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
        if (limit == null || limit !in 1..100) throw BusinessApiException(HttpStatusCode.UnprocessableEntity, StoryListError.Validation(StoryListField.LIMIT, ValidationReason.INVALID_VALUE))
        val cursors = call.request.queryParameters.getAll("cursor")
        val cursor = cursors?.let {
            StoryId.parse(it.singleOrNull()) ?: throw BusinessApiException(HttpStatusCode.BadRequest, StoryListError.InvalidCursor)
        }
        val page = service.list(ownerId, limit, cursor)
            ?: throw BusinessApiException(HttpStatusCode.BadRequest, StoryListError.InvalidCursor)
        call.respond(page.toDto())
    }
    get("/stories/{id}") {
        val story = StoryId.parse(call.parameters["id"])?.let { service.read(call.ownerId(), it) }
            ?: throw BusinessApiException(HttpStatusCode.NotFound, StoryReadError.NotFound)
        call.respond(story.toDto())
    }
}
