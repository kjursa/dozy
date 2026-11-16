package com.dozy.features.story.api.v1

import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusError
import com.dozy.api.contracts.v1.story.ListStoriesRequest
import com.dozy.api.contracts.v1.story.StoryListError
import com.dozy.api.contracts.v1.story.StoryListField
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.features.installation.api.installationId
import com.dozy.features.story.application.StoryService
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.Submission
import com.dozy.plugins.BusinessApiException
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
            Submission.IdempotencyConflict -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationStartError.IdempotencyConflict)
            Submission.ActiveJobExists -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationStartError.ActiveJobExists)
            Submission.ReferenceUnavailable -> throw BusinessApiException(HttpStatusCode.UnprocessableEntity, GenerationStartError.ReferenceUnavailable)
        }
    }
    post("/story-generations/{id}/retry") {
        val installation = call.installationId()
        val key = requireUuid(call.request.headers.getAll("Idempotency-Key")?.singleOrNull(), 4)
        when (val result = service.retry(installation, call.parameters["id"].orEmpty(), key)) {
            is RetrySubmission.Accepted -> call.respond(HttpStatusCode.Accepted, GenerationAcceptedDto(result.job.id, result.job.stage.name))
            RetrySubmission.JobNotFound -> throw BusinessApiException(HttpStatusCode.NotFound, GenerationRetryError.JobNotFound)
            RetrySubmission.JobNotFailed -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationRetryError.JobNotFailed)
            RetrySubmission.ActiveJobExists -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationRetryError.ActiveJobExists)
            RetrySubmission.IdempotencyConflict -> throw BusinessApiException(HttpStatusCode.Conflict, GenerationRetryError.IdempotencyConflict)
        }
    }
    get("/story-generations/{id}") {
        val job = service.status(call.installationId(), call.parameters["id"].orEmpty())
            ?: throw BusinessApiException(HttpStatusCode.NotFound, GenerationStatusError.NotFound)
        call.respond(job.toDto())
    }
    get("/stories") {
        val installation = call.installationId()
        val limits = call.request.queryParameters.getAll("limit")
        val limit = if (limits == null) 100 else limits.singleOrNull()?.takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
        if (limit == null || limit !in 1..100) throw BusinessApiException(HttpStatusCode.UnprocessableEntity, StoryListError.Validation(StoryListField.LIMIT, ValidationReason.INVALID_VALUE))
        val cursors = call.request.queryParameters.getAll("cursor")
        if (cursors != null && (cursors.size != 1 || cursors.single().isBlank())) throw BusinessApiException(HttpStatusCode.BadRequest, StoryListError.InvalidCursor)
        val request = ListStoriesRequest(limit, cursors?.single())
        val page = service.list(installation, request.limit, request.cursor)
            ?: throw BusinessApiException(HttpStatusCode.BadRequest, StoryListError.InvalidCursor)
        call.respond(page.toDto())
    }
    get("/stories/{id}") {
        val story = service.read(call.installationId(), call.parameters["id"].orEmpty())
            ?: throw BusinessApiException(HttpStatusCode.NotFound, StoryReadError.NotFound)
        call.respond(story.toDto())
    }
}
