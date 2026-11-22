package com.dozy.features.story.data.database

import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.domain.model.StorySummary
import com.dozy.features.story.domain.model.StorySync
import com.dozy.features.story.domain.model.readingMinutes
import com.dozy.infrastructure.database.SqliteDatabase
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType

class StoryChangesDao(
    private val database: SqliteDatabase,
) {
    suspend fun sync(
        ownerId: OwnerId,
        after: Long,
    ): StorySync =
        database.write {
            checkNotNull(
                exec(
                    """
                    WITH latest AS (
                        SELECT MAX(revision) AS revision
                        FROM story_changes
                        WHERE owner_id = ? AND revision > ?
                        GROUP BY story_id
                    )
                    SELECT c.revision, c.story_id, c.operation, s.title, s.cover_url, s.text, s.created_at
                    FROM latest
                    JOIN story_changes c ON c.revision = latest.revision
                    LEFT JOIN stories s ON s.id = c.story_id AND s.owner_id = c.owner_id AND c.operation = 'UPSERT'
                    ORDER BY c.revision
                    """.trimIndent(),
                    listOf(TextColumnType() to ownerId.value, LongColumnType() to after),
                    StatementType.SELECT,
                ) { rows ->
                    val upserts = mutableListOf<StorySummary>()
                    val deletedIds = mutableListOf<StoryId>()
                    var nextRevision = after
                    while (rows.next()) {
                        nextRevision = rows.getLong("revision")
                        val id = checkNotNull(StoryId.parse(rows.getString("story_id")))
                        if (rows.getString("operation") == "DELETE") {
                            deletedIds += id
                        } else {
                            upserts +=
                                StorySummary(
                                    id,
                                    rows.getString("title"),
                                    rows.getString("cover_url"),
                                    readingMinutes(rows.getString("text")),
                                    rows.getLong("created_at"),
                                )
                        }
                    }
                    StorySync(upserts, deletedIds, nextRevision)
                },
            )
        }
}
