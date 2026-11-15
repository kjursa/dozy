@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.dozy.features.story.data.repository

import com.dozy.config.GenerationConfig
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.asset.data.database.AssetContents
import com.dozy.features.story.data.database.Stories
import com.dozy.features.story.data.database.StoryJobs
import com.dozy.features.story.data.toDomain
import com.dozy.features.story.data.toDto
import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.SavedStory
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.Submission
import com.dozy.features.story.domain.repository.GenerationRepository
import com.dozy.infrastructure.database.SqliteDatabase
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class SqliteGenerationRepository(private val database: SqliteDatabase, private val config: GenerationConfig) : GenerationRepository {
    override suspend fun start(installation: InstallationId, key: String, canonicalRequest: String, command: StartGeneration): Submission = database.write {
        val existing = StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.key eq key) }.singleOrNull()
        if (existing != null) {
            return@write if (existing[StoryJobs.operation] == "START_STORY_V1" && existing[StoryJobs.canonical] == canonicalRequest) Submission.Accepted(existing.job())
            else Submission.IdempotencyConflict
        }
        if (StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.stage inList active) }.any()) return@write Submission.ActiveJobExists
        for (hero in command.configuration.cast) {
            val avatar = hero.avatar ?: continue
            if (!Assets.selectAll().where { (Assets.id eq avatar.id.lowercase()) and (Assets.installationId eq installation.value) and (Assets.url eq avatar.url) and (Assets.status eq "READY") }.any()) {
                return@write Submission.ReferenceUnavailable
            }
        }
        registerInstallation(installation)
        val id = Uuid.generateV7().toString()
        StoryJobs.insert {
            it[StoryJobs.id] = id
            it[StoryJobs.installation] = installation.value
            it[operation] = "START_STORY_V1"
            it[StoryJobs.key] = key
            it[canonical] = canonicalRequest
            it[configuration] = Json.encodeToString(command.configuration.toDto())
            it[stage] = Stage.QUEUED.name
            it[storyId] = Uuid.generateV7().toString()
            it[assetId] = Uuid.generateV7().toString()
            it[createdAt] = System.currentTimeMillis()
            it[leaseUntil] = 0
            it[executionCount] = 0
        }
        Submission.Accepted(Job(id, Stage.QUEUED))
    }
    override suspend fun status(installation: InstallationId, id: String): Job? = database.write {
        StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.id eq id) }.singleOrNull()?.job()
    }
    override suspend fun read(installation: InstallationId, id: String): SavedStory? = database.write {
        Stories.selectAll().where { (Stories.installation eq installation.value) and (Stories.id eq id) }.singleOrNull()?.let {
            SavedStory(it[Stories.id], it[Stories.title], it[Stories.text], it[Stories.coverUrl], Json.decodeFromString<GenerationRequestDto>(it[Stories.configuration]).toDomain(), it[Stories.createdAt])
        }
    }
    override suspend fun claim(): Execution? = database.write {
        val now = System.currentTimeMillis()
        val row = StoryJobs.selectAll().where { (StoryJobs.stage inList active) and (StoryJobs.leaseUntil lessEq now) }.orderBy(StoryJobs.createdAt).limit(1).singleOrNull() ?: return@write null
        val token = Uuid.random().toString()
        val stage = if (row[StoryJobs.stage] == Stage.QUEUED.name) Stage.CREATING_TEXT else Stage.valueOf(row[StoryJobs.stage])
        StoryJobs.update({ StoryJobs.id eq row[StoryJobs.id] }) {
            it[StoryJobs.stage] = stage.name
            it[leaseToken] = token
            it[leaseUntil] = now + config.leaseMillis
            it[executionCount] = row[StoryJobs.executionCount] + 1
        }
        Execution(Job(row[StoryJobs.id], stage), token, Json.decodeFromString<GenerationRequestDto>(row[StoryJobs.configuration]).toDomain(), row[StoryJobs.title], row[StoryJobs.text])
    }
    override suspend fun renew(execution: Execution): Boolean = database.write {
        StoryJobs.update({ (StoryJobs.id eq execution.job.id) and (StoryJobs.leaseToken eq execution.token) and (StoryJobs.stage inList active) }) {
            it[leaseUntil] = System.currentTimeMillis() + config.leaseMillis
        } == 1
    }
    override suspend fun saveText(execution: Execution, text: GeneratedText): Boolean = database.write {
        StoryJobs.update({ (StoryJobs.id eq execution.job.id) and (StoryJobs.leaseToken eq execution.token) and (StoryJobs.stage eq Stage.CREATING_TEXT.name) }) {
            it[title] = text.title
            it[StoryJobs.text] = text.text
            it[stage] = Stage.CREATING_COVER.name
        } == 1
    }
    override suspend fun publish(execution: Execution, cover: ByteArray): Boolean = database.write {
        val row = StoryJobs.selectAll().where { (StoryJobs.id eq execution.job.id) and (StoryJobs.leaseToken eq execution.token) and (StoryJobs.stage eq Stage.CREATING_COVER.name) }.singleOrNull() ?: return@write false
        val url = "${config.assetBaseUrl.trimEnd('/')}/api/v1/assets/${row[StoryJobs.assetId]}"
        Assets.insert {
            it[id] = row[StoryJobs.assetId]
            it[installationId] = row[StoryJobs.installation]
            it[Assets.url] = url
            it[status] = "READY"
        }
        AssetContents.insert { it[assetId] = row[StoryJobs.assetId]; it[bytes] = cover }
        Stories.insert {
            it[id] = row[StoryJobs.storyId]
            it[jobId] = row[StoryJobs.id]
            it[installation] = row[StoryJobs.installation]
            it[title] = requireNotNull(row[StoryJobs.title])
            it[text] = requireNotNull(row[StoryJobs.text])
            it[coverUrl] = url
            it[configuration] = row[StoryJobs.configuration]
            it[createdAt] = System.currentTimeMillis()
        }
        StoryJobs.update({ StoryJobs.id eq execution.job.id }) { it[stage] = Stage.READY.name; it[leaseToken] = null; it[leaseUntil] = 0 }
        true
    }
    override suspend fun fail(execution: Execution, failure: Failure) { database.write {
        StoryJobs.update({ (StoryJobs.id eq execution.job.id) and (StoryJobs.leaseToken eq execution.token) and (StoryJobs.stage inList active) }) {
            it[stage] = Stage.FAILED.name; it[error] = failure.name; it[leaseToken] = null; it[leaseUntil] = 0
        }
    } }
    private fun ResultRow.job(): Job {
        val stage = Stage.valueOf(this[StoryJobs.stage])
        return Job(this[StoryJobs.id], stage, if (stage == Stage.READY) this[StoryJobs.storyId] else null, this[StoryJobs.error]?.let(Failure::valueOf))
    }
    private companion object { val active = listOf("QUEUED", "CREATING_TEXT", "CREATING_COVER") }
}
