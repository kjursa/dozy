package com.dozy.features.story.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.story.GenerationAcceptedDto
import com.dozy.api.contracts.v1.story.GenerationRetryError
import com.dozy.api.contracts.v1.story.GenerationStartError
import com.dozy.api.contracts.v1.story.GenerationStatusError
import com.dozy.api.contracts.v1.story.StoryListError
import com.dozy.api.contracts.v1.story.StoryListField
import com.dozy.api.contracts.v1.story.StoryReadError
import com.dozy.api.contracts.v1.story.StorySyncError
import com.dozy.domain.id.JobId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.application.StoryService
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.Submission
import com.dozy.plugins.accepted
import com.dozy.plugins.badRequest
import com.dozy.plugins.conflict
import com.dozy.plugins.idempotencyKey
import com.dozy.plugins.notFound
import com.dozy.plugins.ok
import com.dozy.plugins.ownerId
import com.dozy.plugins.unprocessableEntity
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.JsonObject

fun Route.storyV1Routes(service: StoryService) {
    post("/story-generations") {
        val ownerId = call.ownerId()
        val key =
            call.idempotencyKey {
                return@post call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
            } ?: return@post call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
        val body = call.receive<JsonObject>()
        val command =
            when (val parsed = body.command()) {
                null -> return@post call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
                is StartGeneration.Result.Valid -> parsed.command
                is StartGeneration.Result.Invalid ->
                    return@post call.unprocessableEntity(parsed.toApiError())
            }
        when (val result = service.start(ownerId, key, canonicalJson(body), command)) {
            is Submission.Accepted ->
                call.accepted(GenerationAcceptedDto(result.job.id.value, result.job.stage.name))
            Submission.IdempotencyConflict -> call.conflict(GenerationStartError.IdempotencyConflict)
            Submission.ActiveJobExists -> call.conflict(GenerationStartError.ActiveJobExists)
            Submission.ReferenceUnavailable ->
                call.unprocessableEntity(GenerationStartError.ReferenceUnavailable)
        }
    }
    post("/story-generations/{id}/retry") {
        val ownerId = call.ownerId()
        val key =
            call.idempotencyKey {
                return@post call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
            } ?: return@post call.badRequest(ApiErrorResponse(ApiErrorCode.INVALID_REQUEST.value))
        val id =
            JobId.parse(call.parameters["id"])
                ?: return@post call.notFound(GenerationRetryError.JobNotFound)
        when (val result = service.retry(ownerId, id, key)) {
            is RetrySubmission.Accepted ->
                call.accepted(GenerationAcceptedDto(result.job.id.value, result.job.stage.name))
            RetrySubmission.JobNotFound -> call.notFound(GenerationRetryError.JobNotFound)
            RetrySubmission.JobNotFailed -> call.conflict(GenerationRetryError.JobNotFailed)
            RetrySubmission.ActiveJobExists -> call.conflict(GenerationRetryError.ActiveJobExists)
            RetrySubmission.IdempotencyConflict -> call.conflict(GenerationRetryError.IdempotencyConflict)
        }
    }
    get("/story-generations/{id}") {
        val status = JobId.parse(call.parameters["id"])?.let { service.status(call.ownerId(), it) }
        if (status == null) {
            return@get call.notFound(GenerationStatusError.NotFound)
        }
        call.ok(status.toDto())
    }
    get("/stories") {
        val ownerId = call.ownerId()
        val limits = call.request.queryParameters.getAll("limit")
        val limit = if (limits == null) 100 else limits.singleOrNull()?.takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
        if (limit == null ||
            limit !in 1..100
        ) {
            return@get call.unprocessableEntity(StoryListError.Validation(StoryListField.LIMIT, ValidationReason.INVALID_VALUE))
        }
        val cursors = call.request.queryParameters.getAll("cursor")
        val cursor =
            cursors?.let {
                StoryId.parse(it.singleOrNull()) ?: return@get call.badRequest(StoryListError.InvalidCursor)
            }
        val page =
            service.list(ownerId, limit, cursor)
                ?: return@get call.badRequest(StoryListError.InvalidCursor)
        call.ok(page.toDto())
    }
    get("/stories/sync") {
        val ownerId = call.ownerId()
        val revisions = call.request.queryParameters.getAll("after")
        val after = if (revisions == null) 0L else revisions.singleOrNull()?.toLongOrNull()?.takeIf { it >= 0 }
        if (after == null) return@get call.badRequest(StorySyncError.InvalidRevision)
        call.ok(service.sync(ownerId, after).toDto())
    }
    get("/stories/{id}") {
        val story = StoryId.parse(call.parameters["id"])?.let { service.read(call.ownerId(), it) }
        if (story == null) {
            return@get call.notFound(StoryReadError.NotFound)
        }
        call.ok(story.toDto())
    }
}
