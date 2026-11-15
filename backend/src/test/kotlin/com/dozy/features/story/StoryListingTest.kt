package com.dozy.features.story

import com.dozy.config.DatabaseConfig
import com.dozy.config.GenerationConfig
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.story.data.database.Stories
import com.dozy.features.story.data.repository.SqliteGenerationRepository
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.readingMinutes
import com.dozy.infrastructure.database.SqliteDatabase
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.io.TempDir

class StoryListingTest {
    @TempDir lateinit var directory: Path
    private val owner = InstallationId("019a0000-0000-7000-8000-000000000010")
    private val other = InstallationId("019a0000-0000-7000-8000-000000000011")
    private val options = GenerationConfig(delayMillis = 0)

    private suspend fun publish(repository: SqliteGenerationRepository) {
        repository.start(owner, UUID.randomUUID().toString(), "request", command())
        val execution = assertNotNull(repository.claim())
        assertTrue(repository.saveText(execution, GeneratedText("Title", "word ".repeat(151))))
        assertTrue(repository.publish(execution, byteArrayOf(1)))
    }

    @Test fun `snapshot freezes lexical ties across publication and database restart`() = runBlocking {
        val config = DatabaseConfig(directory.resolve("library.db").toString(), 5000)
        lateinit var cursor: String
        lateinit var expected: List<String>
        SqliteDatabase(config).use { database ->
            val repository = SqliteGenerationRepository(database, options)
            assertEquals(emptyList(), repository.list(owner, 2, null)?.stories)
            repeat(5) { publish(repository) }
            database.write { Stories.update { it[createdAt] = 123 } }
            val all = assertNotNull(repository.list(owner, 100, null))
            expected = all.stories.map { it.id }
            assertEquals(expected.sorted(), expected)
            assertTrue(all.stories.all { it.readingMinutes == 2 && it.createdAt == 123L })
            val first = assertNotNull(repository.list(owner, 2, null))
            assertEquals(expected.take(2), first.stories.map { it.id })
            cursor = assertNotNull(first.nextCursor)
            publish(repository)
            assertNull(repository.list(other, 2, cursor))
            assertNull(repository.list(owner, 3, cursor))
            assertNull(repository.list(owner, 2, "malformed /+"))
        }
        SqliteDatabase(config).use { database ->
            val repository = SqliteGenerationRepository(database, options)
            val second = assertNotNull(repository.list(owner, 2, cursor))
            assertEquals(expected.drop(2).take(2), second.stories.map { it.id })
            assertEquals(second, repository.list(owner, 2, cursor))
            val last = assertNotNull(repository.list(owner, 2, second.nextCursor))
            assertEquals(expected.takeLast(1), last.stories.map { it.id })
            assertNull(last.nextCursor)
            assertEquals(6, repository.list(owner, 100, null)?.stories?.size)
            assertTrue(repository.list(other, 100, null)?.stories?.isEmpty() == true)
        }
    }

    @Test fun `reading minutes count whitespace words and round up exactly`() {
        for ((text, expected) in listOf("" to 1, " \n\t " to 1, "word" to 1, "word\t".repeat(150) to 1, "word\n".repeat(151) to 2, "word ".repeat(300) to 2, "word ".repeat(301) to 3)) {
            assertEquals(expected, readingMinutes(text))
        }
    }
}
