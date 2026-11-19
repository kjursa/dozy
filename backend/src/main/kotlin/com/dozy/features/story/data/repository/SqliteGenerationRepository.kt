package com.dozy.features.story.data.repository

import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.config.GenerationConfig
import com.dozy.domain.id.AssetId
import com.dozy.domain.id.GenerationId
import com.dozy.domain.id.IdempotencyKey
import com.dozy.domain.id.JobId
import com.dozy.domain.id.LeaseToken
import com.dozy.domain.id.LibraryCursor
import com.dozy.domain.id.LibrarySnapshotId
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.asset.data.database.AssetContents
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.installation.data.database.registerInstallation
import com.dozy.features.story.data.database.LibraryCursors
import com.dozy.features.story.data.database.LibrarySnapshots
import com.dozy.features.story.data.database.Stories
import com.dozy.features.story.data.database.StoryExecutions
import com.dozy.features.story.data.database.StoryJobs
import com.dozy.features.story.data.toDomain
import com.dozy.features.story.data.toDto
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.Job
import com.dozy.features.story.domain.model.RetrySubmission
import com.dozy.features.story.domain.model.SavedStory
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.model.StartGeneration
import com.dozy.features.story.domain.model.StoryPage
import com.dozy.features.story.domain.model.StorySummary
import com.dozy.features.story.domain.model.Submission
import com.dozy.features.story.domain.model.readingMinutes
import com.dozy.features.story.domain.repository.GenerationRepository
import com.dozy.infrastructure.database.SqliteDatabase
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock

class SqliteGenerationRepository(
    private val database: SqliteDatabase,
    private val config: GenerationConfig,
    private val clock: Clock,
) : GenerationRepository {
    override suspend fun list(installation: OwnerId, limit: Int, cursor: LibraryCursor?): StoryPage? = database.write {
        require(limit in 1..100)
        val snapshotId: LibrarySnapshotId
        val position: Int
        val summaries: List<StorySummaryDto>
        if (cursor == null) {
            registerInstallation(installation)
            snapshotId = LibrarySnapshotId.generate()
            position = 0
            summaries = Stories.selectAll().where { Stories.installation eq installation.value }.map {
                StorySummaryDto(it[Stories.id], it[Stories.title], it[Stories.coverUrl], readingMinutes(it[Stories.text]), it[Stories.createdAt])
            }.sortedWith(compareByDescending<StorySummaryDto> { it.createdAt }.thenBy { it.id })
            LibrarySnapshots.insert {
                it[id] = snapshotId.value
                it[LibrarySnapshots.installation] = installation.value
                it[LibrarySnapshots.limit] = limit
                it[LibrarySnapshots.summaries] = Json.encodeToString(summaries)
            }
        } else {
            val savedCursor = LibraryCursors.selectAll().where { LibraryCursors.token eq cursor.value }.singleOrNull() ?: return@write null
            snapshotId = requireNotNull(LibrarySnapshotId.parse(savedCursor[LibraryCursors.snapshot]))
            position = savedCursor[LibraryCursors.position]
            val saved = LibrarySnapshots.selectAll().where { (LibrarySnapshots.id eq snapshotId.value) and (LibrarySnapshots.installation eq installation.value) and (LibrarySnapshots.limit eq limit) }.singleOrNull() ?: return@write null
            summaries = Json.decodeFromString(saved[LibrarySnapshots.summaries])
        }
        val end = minOf(position + limit, summaries.size)
        val next = if (end < summaries.size) {
            LibraryCursors.selectAll().where { (LibraryCursors.snapshot eq snapshotId.value) and (LibraryCursors.position eq end) }.singleOrNull()?.let { requireNotNull(LibraryCursor.parse(it[LibraryCursors.token])) }
                ?: LibraryCursor.generate().also { token -> LibraryCursors.insert { it[LibraryCursors.token] = token.value; it[snapshot] = snapshotId.value; it[LibraryCursors.position] = end } }
        } else null
        StoryPage(summaries.subList(position, end).map { StorySummary(requireNotNull(StoryId.parse(it.id)), it.title, it.coverUrl, it.readingMinutes, it.createdAt) }, next)
    }

    override suspend fun start(installation: OwnerId, key: IdempotencyKey, canonicalRequest: String, command: StartGeneration): Submission = database.write {
        val existing = StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.key eq key.value) }.singleOrNull()
        if (existing != null) {
            return@write if (existing[StoryJobs.operation] == "START_STORY_V1" && existing[StoryJobs.canonical] == canonicalRequest) Submission.Accepted(existing.job())
            else Submission.IdempotencyConflict
        }
        if (StoryExecutions.selectAll().where { (StoryExecutions.installation eq installation.value) and (StoryExecutions.key eq key.value) }.any()) return@write Submission.IdempotencyConflict
        if (StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.stage inList active) }.any()) return@write Submission.ActiveJobExists
        for (hero in command.configuration.cast) {
            val avatar = hero.avatar ?: continue
            if (!Assets.selectAll().where { (Assets.id eq avatar.id.value) and (Assets.installationId eq installation.value) and (Assets.url eq avatar.url) and (Assets.status eq "READY") }.any()) {
                return@write Submission.ReferenceUnavailable
            }
        }
        registerInstallation(installation)
        val id = JobId.generate()
        val executionId = GenerationId.generate()
        StoryJobs.insert {
            it[StoryJobs.id] = id.value
            it[StoryJobs.installation] = installation.value
            it[operation] = "START_STORY_V1"
            it[StoryJobs.key] = key.value
            it[canonical] = canonicalRequest
            it[configuration] = Json.encodeToString(command.configuration.toDto())
            it[stage] = Stage.QUEUED.name
            it[storyId] = StoryId.generate().value
            it[assetId] = AssetId.generate().value
            it[createdAt] = clock.millis()
            it[leaseUntil] = 0
            it[StoryJobs.executionId] = executionId.value
            it[ordinal] = 1
            it[executionCount] = 0
        }
        StoryExecutions.insert {
            it[StoryExecutions.id] = executionId.value
            it[StoryExecutions.installation] = installation.value
            it[jobId] = id.value
            it[StoryExecutions.key] = key.value
            it[operation] = "START_STORY_V1"
            it[ordinal] = 1
            it[stage] = Stage.QUEUED.name
        }
        Submission.Accepted(Job(id, Stage.QUEUED))
    }
    override suspend fun retry(installation: OwnerId, id: JobId, key: IdempotencyKey): RetrySubmission = database.write {
        val binding = StoryExecutions.selectAll().where { (StoryExecutions.installation eq installation.value) and (StoryExecutions.key eq key.value) }.singleOrNull()
        if (binding != null) {
            return@write if (binding[StoryExecutions.operation] == "RETRY_STORY_V1" && binding[StoryExecutions.jobId] == id.value) {
                RetrySubmission.Accepted(Job(id, Stage.valueOf(binding[StoryExecutions.stage])))
            } else RetrySubmission.IdempotencyConflict
        }
        val row = StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.id eq id.value) }.singleOrNull()
            ?: return@write RetrySubmission.JobNotFound
        if (row[StoryJobs.stage] != Stage.FAILED.name) return@write RetrySubmission.JobNotFailed
        if (StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.stage inList active) }.any()) return@write RetrySubmission.ActiveJobExists
        val executionId = GenerationId.generate()
        val ordinal = row[StoryJobs.ordinal] + 1
        StoryExecutions.insert {
            it[StoryExecutions.id] = executionId.value
            it[StoryExecutions.installation] = installation.value
            it[jobId] = id.value
            it[StoryExecutions.key] = key.value
            it[operation] = "RETRY_STORY_V1"
            it[StoryExecutions.ordinal] = ordinal
            it[stage] = Stage.QUEUED.name
        }
        StoryJobs.update({ StoryJobs.id eq id.value }) {
            it[StoryJobs.executionId] = executionId.value
            it[StoryJobs.ordinal] = ordinal
            it[stage] = Stage.QUEUED.name
            it[title] = null; it[text] = null; it[error] = null
            it[leaseToken] = null; it[leaseUntil] = 0
        }
        RetrySubmission.Accepted(Job(id, Stage.QUEUED))
    }
    private fun recordStage(id: JobId) {
        val row = StoryJobs.selectAll().where { StoryJobs.id eq id.value }.single()
        StoryExecutions.update({ StoryExecutions.id eq row[StoryJobs.executionId] }) { it[stage] = row[StoryJobs.stage] }
    }
    override suspend fun status(installation: OwnerId, id: JobId): Job? = database.write {
        StoryJobs.selectAll().where { (StoryJobs.installation eq installation.value) and (StoryJobs.id eq id.value) }.singleOrNull()?.job()
    }
    override suspend fun read(installation: OwnerId, id: StoryId): SavedStory? = database.write {
        Stories.selectAll().where { (Stories.installation eq installation.value) and (Stories.id eq id.value) }.singleOrNull()?.let {
            SavedStory(requireNotNull(StoryId.parse(it[Stories.id])), it[Stories.title], it[Stories.text], it[Stories.coverUrl], decodeConfiguration(it[Stories.configuration]), it[Stories.createdAt])
        }
    }
    override suspend fun claim(): Execution? = database.write {
        val now = clock.millis()
        val row = StoryJobs.selectAll().where { (StoryJobs.stage inList active) and (StoryJobs.leaseUntil lessEq now) }.orderBy(StoryJobs.createdAt).limit(1).singleOrNull() ?: return@write null
        val token = LeaseToken.generate()
        val stage = if (row[StoryJobs.stage] == Stage.QUEUED.name) Stage.CREATING_TEXT else Stage.valueOf(row[StoryJobs.stage])
        StoryJobs.update({ StoryJobs.id eq row[StoryJobs.id] }) {
            it[StoryJobs.stage] = stage.name
            it[leaseToken] = token.value
            it[leaseUntil] = now + config.leaseMillis
            it[executionCount] = row[StoryJobs.executionCount] + 1
        }
        val jobId = requireNotNull(JobId.parse(row[StoryJobs.id]))
        recordStage(jobId)
        Execution(Job(jobId, stage), token, decodeConfiguration(row[StoryJobs.configuration]), row[StoryJobs.title], row[StoryJobs.text], row[StoryJobs.ordinal], requireNotNull(GenerationId.parse(row[StoryJobs.executionId])))
    }
    override suspend fun renew(execution: Execution): Boolean = database.write {
        StoryJobs.update({ (StoryJobs.id eq execution.job.id.value) and (StoryJobs.executionId eq execution.id.value) and (StoryJobs.leaseToken eq execution.token.value) and (StoryJobs.stage inList active) }) {
            it[leaseUntil] = clock.millis() + config.leaseMillis
        } == 1
    }
    override suspend fun saveText(execution: Execution, text: GeneratedText): Boolean = database.write {
        val changed = StoryJobs.update({ (StoryJobs.id eq execution.job.id.value) and (StoryJobs.executionId eq execution.id.value) and (StoryJobs.leaseToken eq execution.token.value) and (StoryJobs.stage eq Stage.CREATING_TEXT.name) }) {
            it[title] = text.title
            it[StoryJobs.text] = text.text
            it[stage] = Stage.CREATING_COVER.name
        } == 1
        recordStage(execution.job.id)
        changed
    }
    override suspend fun publish(execution: Execution, cover: ByteArray): Boolean = database.write {
        val row = StoryJobs.selectAll().where { (StoryJobs.id eq execution.job.id.value) and (StoryJobs.executionId eq execution.id.value) and (StoryJobs.leaseToken eq execution.token.value) and (StoryJobs.stage eq Stage.CREATING_COVER.name) }.singleOrNull() ?: return@write false
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
            it[createdAt] = clock.millis()
        }
        StoryJobs.update({ StoryJobs.id eq execution.job.id.value }) { it[stage] = Stage.READY.name; it[leaseToken] = null; it[leaseUntil] = 0 }
        recordStage(execution.job.id)
        true
    }
    override suspend fun fail(execution: Execution, failure: Failure) { database.write {
        StoryJobs.update({ (StoryJobs.id eq execution.job.id.value) and (StoryJobs.executionId eq execution.id.value) and (StoryJobs.leaseToken eq execution.token.value) and (StoryJobs.stage inList active) }) {
            it[stage] = Stage.FAILED.name; it[error] = failure.name; it[leaseToken] = null; it[leaseUntil] = 0
        }
        recordStage(execution.job.id)
    } }
    private fun ResultRow.job(): Job {
        val stage = Stage.valueOf(this[StoryJobs.stage])
        return Job(requireNotNull(JobId.parse(this[StoryJobs.id])), stage, if (stage == Stage.READY) requireNotNull(StoryId.parse(this[StoryJobs.storyId])) else null, this[StoryJobs.error]?.let(Failure::valueOf))
    }
    private fun decodeConfiguration(json: String) = requireNotNull(Json.decodeFromString<GenerationRequestDto>(json).toDomain())

    private companion object { val active = listOf("QUEUED", "CREATING_TEXT", "CREATING_COVER") }
}
