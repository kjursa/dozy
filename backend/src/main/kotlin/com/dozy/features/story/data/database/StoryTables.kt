package com.dozy.features.story.data.database

import org.jetbrains.exposed.v1.core.Table

internal object StoryJobs : Table("story_jobs") {
    val id = text("id")
    val installation = text("installation_id")
    val operation = text("operation")
    val key = text("idempotency_key")
    val canonical = text("canonical_request")
    val configuration = text("configuration")
    val stage = text("stage")
    val storyId = text("story_id")
    val assetId = text("asset_id")
    val createdAt = long("created_at")
    val title = text("title").nullable()
    val text = text("text").nullable()
    val error = text("error_code").nullable()
    val leaseToken = text("lease_token").nullable()
    val leaseUntil = long("lease_until")
    val executionCount = integer("execution_count")
    override val primaryKey = PrimaryKey(id)
}
internal object Stories : Table("stories") {
    val id = text("id")
    val jobId = text("job_id")
    val installation = text("installation_id")
    val title = text("title")
    val text = text("text")
    val coverUrl = text("cover_url")
    val configuration = text("configuration")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}
