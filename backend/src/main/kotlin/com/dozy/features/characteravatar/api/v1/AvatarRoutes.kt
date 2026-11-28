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
import com.dozy.plugins.accepted
import com.dozy.plugins.notFound
import com.dozy.plugins.ok
import com.dozy.plugins.ownerId
import com.dozy.plugins.unprocessableEntity
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.avatarV1Routes(jobs: AvatarJobs) {
    route("/character-avatars") {
        post {
            val ownerId = call.ownerId()
            val request = call.receive<AvatarRequestDto>()
            val input =
                when (val parsed = AvatarInput.parse(request.type, request.personality, request.appearance)) {
                    is AvatarInput.Result.Valid -> parsed.input
                    is AvatarInput.Result.Invalid ->
                        return@post call.unprocessableEntity(
                            AvatarStartError.invalidField(
                                AvatarField.valueOf(parsed.field.name),
                                ValidationReason.valueOf(parsed.reason.name),
                            ),
                        )
                }
            call.accepted(jobs.create(ownerId, input).toDto())
        }
        get("/{jobId}") {
            val job =
                AvatarJobId.parse(call.parameters["jobId"])?.let { jobs.find(call.ownerId(), it) }
                    ?: return@get call.notFound(AvatarStatusError.NotFound)
            call.ok(job.toDto())
        }
    }
}

fun Route.avatarAssetRoutes(repository: AssetRepository) {
    get("/assets/{assetId}") {
        val bytes = AssetId.parse(call.parameters["assetId"])?.let { repository.read(call.ownerId(), it) }
        if (bytes == null) return@get call.notFound()
        call.response.headers.append(HttpHeaders.CacheControl, "private, no-store")
        call.respondBytes(bytes, ContentType.Image.PNG)
    }
}

private fun AvatarJob.toDto() =
    AvatarJobDto(
        id.value,
        AvatarJobState.valueOf(state.name).name,
        if (state == AvatarState.READY) AvatarDto(assetId.value, url) else null,
        if (state == AvatarState.FAILED) AvatarFailureCode.GENERATION_FAILED.name else null,
    )
