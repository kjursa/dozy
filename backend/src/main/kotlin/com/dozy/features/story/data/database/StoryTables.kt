package com.dozy.features.story.data.database

import org.jetbrains.exposed.v1.core.Table

internal object StoryJobs : Table("story_jobs") {
    val id = text("id")
    val ownerId = text("owner_id")
    val operation = text("operation")
    val key = text("idempotency_key")
    val canonical = text("canonical_request")
    val configuration = text("configuration")
    val stage = text("stage")
    val assetId = text("asset_id")
    val createdAt = long("created_at")
    val title = text("title").nullable()
    val text = text("text").nullable()
    val error = text("error_code").nullable()
    val leaseToken = text("lease_token").nullable()
    val leaseUntil = long("lease_until")
    val executionId = text("execution_id")
    val ordinal = integer("ordinal")
    val executionCount = integer("execution_count")
    override val primaryKey = PrimaryKey(id)
}
internal object Stories : Table("stories") {
    val id = text("id")
    val jobId = text("job_id")
    val ownerId = text("owner_id")
    val title = text("title")
    val text = text("text")
    val coverUrl = text("cover_url")
    val configuration = text("configuration")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

internal object StoryExecutions : Table("story_executions") {
    val id = text("id")
    val ownerId = text("owner_id")
    val jobId = text("job_id")
    val key = text("idempotency_key")
    val operation = text("operation")
    val ordinal = integer("ordinal")
    val stage = text("stage")
    override val primaryKey = PrimaryKey(id)
}
