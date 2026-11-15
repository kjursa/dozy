package com.dozy.features.characteravatar.api.v1

import com.dozy.api.contracts.v1.ApiErrorCode
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.characteravatar.AvatarField
import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarJobState
import com.dozy.api.contracts.v1.characteravatar.AvatarFailureCode
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.model.AvatarJob
import com.dozy.features.characteravatar.domain.model.AvatarState
import com.dozy.features.characteravatar.domain.repository.AvatarJobs
import com.dozy.features.characteravatar.domain.repository.AvatarStorage
import com.dozy.features.installation.api.installationId
import com.dozy.plugins.ApiException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.avatarV1Routes(jobs: AvatarJobs) {
    route("/character-avatars") {
        post {
            val owner = call.installationId()
            val request = call.receive<AvatarRequestDto>()
            val input = when (val parsed = AvatarInput.parse(request.type, request.personality, request.appearance)) {
                is AvatarInput.Result.Valid -> parsed.input
                is AvatarInput.Result.Invalid -> throw ApiException(HttpStatusCode.UnprocessableEntity, ApiErrorCode.VALIDATION_FAILED,
                    AvatarField.valueOf(parsed.field.name).value, ValidationReason.valueOf(parsed.reason.name).value)
            }
            call.respond(HttpStatusCode.Accepted, jobs.create(owner, input).toDto())
        }
        get("/{jobId}") {
            val job = jobs.find(call.installationId(), call.parameters["jobId"].orEmpty())
                ?: throw ApiException(HttpStatusCode.NotFound, ApiErrorCode.AVATAR_JOB_NOT_FOUND)
            call.respond(job.toDto())
        }
    }
}
fun Route.avatarAssetRoutes(storage: AvatarStorage) {
    get("/assets/{assetId}") {
        val bytes = storage.get(call.parameters["assetId"].orEmpty())
        if (bytes == null) call.respond(HttpStatusCode.NotFound)
        else {
            call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            call.respondBytes(bytes, ContentType.Image.PNG)
        }
    }
}
private fun AvatarJob.toDto() = AvatarJobDto(id, AvatarJobState.valueOf(state.name).name,
    if (state == AvatarState.READY) AvatarDto(assetId, url) else null,
    if (state == AvatarState.FAILED) AvatarFailureCode.GENERATION_FAILED.name else null)
