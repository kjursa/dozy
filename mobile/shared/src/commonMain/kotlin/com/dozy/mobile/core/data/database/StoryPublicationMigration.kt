package com.dozy.mobile.core.data.database

import androidx.room3.migration.Migration
import androidx.sqlite.execSQL

internal val storyPublicationMigration =
    Migration(9, 10) { connection ->
        connection.execSQL(
            """
            CREATE TABLE stories (
                id TEXT NOT NULL PRIMARY KEY, jobId TEXT, requestKey TEXT, result TEXT, summary TEXT,
                notificationDelivered INTEGER NOT NULL, createdOrder INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT OR REPLACE INTO stories (id, jobId, requestKey, result, notificationDelivered, createdOrder)
            SELECT json_extract(result, '$.id'), jobId, CASE WHEN jobId IS NOT NULL THEN requestKey END,
                   result, notificationDelivered, createdOrder
            FROM story_generations WHERE result IS NOT NULL
            ORDER BY jobId IS NOT NULL, createdOrder
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT INTO stories (id, summary, notificationDelivered, createdOrder)
            SELECT json_extract(summary, '$.id'), summary, 0, 0
            FROM story_generations WHERE summary IS NOT NULL
            ON CONFLICT(id) DO UPDATE SET summary = excluded.summary
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX index_stories_jobId ON stories(jobId)")
        connection.execSQL("CREATE UNIQUE INDEX index_stories_requestKey ON stories(requestKey)")
        connection.execSQL("DELETE FROM story_generations WHERE result IS NOT NULL OR summary IS NOT NULL")
        connection.execSQL("ALTER TABLE story_generations DROP COLUMN result")
        connection.execSQL("ALTER TABLE story_generations DROP COLUMN summary")
        connection.execSQL("ALTER TABLE story_generations DROP COLUMN notificationDelivered")
    }
