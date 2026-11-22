package com.dozy.features.story

import com.dozy.config.DatabaseConfig
import com.dozy.config.GenerationConfig
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.StoryId
import com.dozy.features.story.application.StoryService
import com.dozy.features.story.data.database.Stories
import com.dozy.features.story.data.repository.SqliteGenerationRepository
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.readingMinutes
import com.dozy.infrastructure.database.SqliteDatabase
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoryListingTest {
    private val clock = Clock.systemUTC()
    @TempDir lateinit var directory: Path
    private val owner = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000010"))
    private val other = requireNotNull(OwnerId.parse("owner-019a0000-0000-7000-8000-000000000011"))
    private val options = GenerationConfig(delayMillis = 0)

    private suspend fun publish(repository: SqliteGenerationRepository, owner: OwnerId = this.owner): StoryId {
        repository.start(owner, requestKey(), "request", command())
        val execution = assertNotNull(repository.claim())
        assertNull(repository.status(owner, execution.job.id)?.storyId)
        assertTrue(repository.saveText(execution, GeneratedText("Title", "word ".repeat(151))))
        return assertNotNull(repository.publish(execution, byteArrayOf(1)))
    }

    @Test fun `story ID cursor continues across new publication and database restart`() = runBlocking {
        val config = DatabaseConfig(directory.resolve("library.db").toString(), 5000)
        var cursor: StoryId? = null
        lateinit var expected: List<String>
        SqliteDatabase(config).use { database ->
            val repository = testGenerationRepository(database, options, clock)
            assertEquals(emptyList(), repository.list(owner, 2, null)?.stories)
            expected = List(5) { publish(repository).value }.sortedDescending()
            database.write { Stories.update { it[createdAt] = 123 } }
            val all = assertNotNull(repository.list(owner, 100, null))
            assertEquals(expected, all.stories.map { it.id.value })
            assertTrue(all.stories.all { it.readingMinutes == 2 && it.createdAt == 123L })
            val first = assertNotNull(repository.list(owner, 2, null))
            assertEquals(expected.take(2), first.stories.map { it.id.value })
            cursor = assertNotNull(first.nextCursor)
            assertEquals(first.stories.last().id, cursor)
            val newer = publish(repository)
            assertEquals(newer, repository.list(owner, 1, null)?.stories?.single()?.id)
            assertNull(repository.list(other, 2, cursor))
            assertEquals(expected.drop(2), repository.list(owner, 3, cursor)?.stories?.map { it.id.value })
            assertNull(repository.list(owner, 2, StoryId.generate()))
        }
        SqliteDatabase(config).use { database ->
            val repository = testGenerationRepository(database, options, clock)
            val second = assertNotNull(repository.list(owner, 2, cursor))
            assertEquals(expected.drop(2).take(2), second.stories.map { it.id.value })
            assertEquals(second, repository.list(owner, 2, cursor))
            val last = assertNotNull(repository.list(owner, 2, second.nextCursor))
            assertEquals(expected.takeLast(1), last.stories.map { it.id.value })
            assertNull(last.nextCursor)
            assertEquals(6, repository.list(owner, 100, null)?.stories?.size)
            assertTrue(repository.list(other, 100, null)?.stories?.isEmpty() == true)
        }
    }

    @Test fun `revision sync exposes committed changes for only one owner`() =
        runBlocking {
            val config = DatabaseConfig(directory.resolve("library.db").toString(), 5000)
            SqliteDatabase(config).use { database ->
                val repository = testGenerationRepository(database, options, clock)
                val first = publish(repository)
                val initial = repository.sync(owner, 0)
                assertEquals(listOf(first), initial.upserts.map { it.id })
                assertTrue(initial.nextRevision > 0)
                val foreign = publish(repository, other)
                val second = publish(repository)
                val secondJob = assertNotNull(repository.read(owner, second)).jobId
                val delta = repository.sync(owner, initial.nextRevision)
                assertEquals(listOf(second), delta.upserts.map { it.id })
                assertEquals(listOf(foreign), repository.sync(other, 0).upserts.map { it.id })
                assertFailsWith<IllegalStateException> {
                    database.write {
                        Stories.deleteWhere { id eq second.value }
                        error("rollback")
                    }
                }
                assertEquals(delta, repository.sync(owner, initial.nextRevision))

                database.write {
                    Stories.update({ Stories.id eq first.value }) { it[title] = "Revised" }
                    Stories.deleteWhere { id eq second.value }
                }
                val changes = repository.sync(owner, initial.nextRevision)
                assertEquals(listOf(first), changes.upserts.map { it.id })
                assertEquals("Revised", changes.upserts.single().title)
                assertEquals(listOf(second), changes.deletedIds)
                assertTrue(changes.nextRevision > delta.nextRevision)
                val unchanged = repository.sync(owner, changes.nextRevision)
                assertTrue(unchanged.upserts.isEmpty() && unchanged.deletedIds.isEmpty())
                assertEquals(changes.nextRevision, unchanged.nextRevision)
                assertNull(repository.read(owner, second))
                assertNull(StoryService(repository).status(owner, secondJob))
            }
        }

    @Test fun `reading minutes count whitespace words and round up exactly`() {
        for ((text, expected) in listOf("" to 1, " \n\t " to 1, "word" to 1, "word\t".repeat(150) to 1, "word\n".repeat(151) to 2, "word ".repeat(300) to 2, "word ".repeat(301) to 3)) {
            assertEquals(expected, readingMinutes(text))
        }
    }
}
