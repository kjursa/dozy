package com.dozy.features.characteravatar.api.v1

import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.api.contracts.v1.characteravatar.AvatarFailureCode
import com.dozy.api.contracts.v1.characteravatar.AvatarField
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarJobState
import com.dozy.api.contracts.v1.characteravatar.AvatarRequestDto
import com.dozy.api.contracts.v1.characteravatar.AvatarStartError
import com.dozy.api.contracts.v1.characteravatar.AvatarStatusError
import com.dozy.domain.id.AssetId
import com.dozy.domain.id.AvatarJobId
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.model.AvatarJob
import com.dozy.features.characteravatar.domain.model.AvatarState
import com.dozy.features.characteravatar.domain.repository.AvatarJobs
import com.dozy.plugins.BusinessApiException
import com.dozy.plugins.ownerId
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
            val owner = call.ownerId()
            val request = call.receive<AvatarRequestDto>()
            val input = when (val parsed = AvatarInput.parse(request.type, request.personality, request.appearance)) {
                is AvatarInput.Result.Valid -> parsed.input
                is AvatarInput.Result.Invalid -> throw BusinessApiException(HttpStatusCode.UnprocessableEntity, AvatarStartError.Validation(
                    AvatarField.valueOf(parsed.field.name), ValidationReason.valueOf(parsed.reason.name)))
            }
            call.respond(HttpStatusCode.Accepted, jobs.create(owner, input).toDto())
        }
        get("/{jobId}") {
            val job = AvatarJobId.parse(call.parameters["jobId"])?.let { jobs.find(call.ownerId(), it) }
                ?: throw BusinessApiException(HttpStatusCode.NotFound, AvatarStatusError.NotFound)
            call.respond(job.toDto())
        }
    }
}
fun Route.avatarAssetRoutes(repository: AssetRepository) {
    get("/assets/{assetId}") {
        val bytes = AssetId.parse(call.parameters["assetId"])?.let { repository.read(call.ownerId(), it) }
        if (bytes == null) call.respond(HttpStatusCode.NotFound)
        else {
            call.response.headers.append(HttpHeaders.CacheControl, "private, no-store")
            call.respondBytes(bytes, ContentType.Image.PNG)
        }
    }
}
private fun AvatarJob.toDto() = AvatarJobDto(id.value, AvatarJobState.valueOf(state.name).name,
    if (state == AvatarState.READY) AvatarDto(assetId.value, url) else null,
    if (state == AvatarState.FAILED) AvatarFailureCode.GENERATION_FAILED.name else null)
