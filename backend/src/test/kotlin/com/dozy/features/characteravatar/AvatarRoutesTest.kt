package com.dozy.features.characteravatar

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.characteravatar.AvatarField
import com.dozy.api.contracts.v1.characteravatar.AvatarJobDto
import com.dozy.api.contracts.v1.characteravatar.AvatarStartError
import com.dozy.asOwner
import com.dozy.config.AvatarConfig
import com.dozy.config.FakeAvatarMode
import com.dozy.databaseConfig
import com.dozy.domain.id.OwnerId
import com.dozy.module
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AvatarRoutesTest {
    @TempDir lateinit var directory: Path
    private val owner = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000001"))
    private val stranger = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000002"))
    private val request = """{"type":"CHILD","personality":"Kind","appearance":"Green eyes"}"""

    @Test fun acceptedJobsServeValidPngAndOwnedCharacterReference() = testApplication {
        environment { config = databaseConfig(directory) }
        application {
            dependencies.provide<AvatarConfig> { AvatarConfig(storagePath = directory.resolve("assets").toString(), delayMillis = 0) }
            module()
        }
        val accepted = client.post("/api/v1/character-avatars") {
            asOwner(owner); contentType(ContentType.Application.Json); setBody(request)
        }
        assertEquals(HttpStatusCode.Accepted, accepted.status)
        var job = Json.decodeFromString<AvatarJobDto>(accepted.bodyAsText())
        assertEquals("QUEUED", job.status)
        assertNull(job.asset); assertNull(job.error)
        for (attempt in 0 until 100) {
            val status = client.get("/api/v1/character-avatars/${job.jobId}") { asOwner(owner) }
            assertEquals(HttpStatusCode.OK, status.status)
            job = Json.decodeFromString(status.bodyAsText())
            if (job.status == "READY") break
            delay(20)
        }
        assertEquals("READY", job.status)
        val asset = assertNotNull(job.asset)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/assets/${asset.id}").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/assets/${asset.id}") { asOwner(stranger) }.status)
        val image = client.get("/assets/${asset.id}") { asOwner(owner) }
        assertEquals(HttpStatusCode.OK, image.status)
        assertEquals("image/png", image.headers["Content-Type"])
        assertEquals(256, ImageIO.read(ByteArrayInputStream(image.bodyAsBytes())).height)
        assertEquals(HttpStatusCode.NotFound, client.get("/assets/unknown") { asOwner(owner) }.status)
        for (id in listOf(job.jobId, "unknown")) {
            val missing = client.get("/api/v1/character-avatars/$id") { asOwner(stranger) }
            assertEquals(HttpStatusCode.NotFound, missing.status)
            assertEquals("AVATAR_JOB_NOT_FOUND", Json.decodeFromString<ApiErrorResponse>(missing.bodyAsText()).code)
        }
        val character = """{"name":"Hero","type":"CHILD","personality":"Kind","appearance":"Green eyes","avatar":${Json.encodeToString(
            asset
        )}}"""
        assertEquals(
            HttpStatusCode.Created,
            client.post("/api/v1/characters") {
                asOwner(owner)
                contentType(ContentType.Application.Json)
                setBody(character)
            }.status
        )
        val invalid = client.post("/api/v1/character-avatars") {
            asOwner(owner); contentType(ContentType.Application.Json); setBody(request.replace("Kind", ""))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, invalid.status)
        assertEquals(AvatarStartError.Validation(AvatarField.PERSONALITY, ValidationReason.REQUIRED), Json.decodeFromString<AvatarStartError>(invalid.bodyAsText()))
    }

    @Test fun failedJobIsSuccessfulStatusWithContractError() = testApplication {
        environment { config = databaseConfig(directory) }
        application {
            dependencies.provide<AvatarConfig> { AvatarConfig(storagePath = directory.resolve("assets").toString(), mode = FakeAvatarMode.FAILURE, delayMillis = 0) }
            module()
        }
        val accepted = client.post("/api/v1/character-avatars") {
            asOwner(owner); contentType(ContentType.Application.Json); setBody(request)
        }
        var job = Json.decodeFromString<AvatarJobDto>(accepted.bodyAsText())
        for (attempt in 0 until 100) {
            val status = client.get("/api/v1/character-avatars/${job.jobId}") { asOwner(owner) }
            assertEquals(HttpStatusCode.OK, status.status)
            job = Json.decodeFromString(status.bodyAsText())
            if (job.status == "FAILED") break
            delay(20)
        }
        assertEquals("FAILED", job.status)
        assertEquals("GENERATION_FAILED", job.error)
        assertNull(job.asset)
    }
}
