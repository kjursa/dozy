package com.dozy.mobile

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room3.Room
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.dozy.api.contracts.v1.story.StoryPageDto
import com.dozy.api.contracts.v1.story.StorySummaryDto
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.background.AndroidStoryCompletionNotifier
import com.dozy.mobile.feature.story.data.database.GenerationEntity
import com.dozy.mobile.feature.story.data.database.StoryEntity
import com.dozy.mobile.feature.story.data.testGenerationRepository
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.presentation.generation.notificationStoryId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GenerationPersistenceTest {
    @Before fun initializeSharedResources() {
        initializeComposeResourceProvider()
    }

    @Test fun libraryReplacementIsAtomicAndLeavesPendingGenerationsIndependent() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            val database = Room.inMemoryDatabaseBuilder<DozyDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
            try {
                val dao = database.generations()
                val pending = GenerationEntity("pending", "configuration", jobId = "job", status = "QUEUED")
                val old = StoryEntity("old", summary = "old")
                dao.upsert(pending)
                dao.replaceLibrary(listOf(old))
                try {
                    database.withWriteTransaction {
                        dao.replaceLibrary(listOf(StoryEntity("new", summary = "new")))
                        throw CancellationException("cancel transaction")
                    }
                    fail("Cancellation must propagate")
                } catch (_: CancellationException) {
                }
                assertEquals(listOf(pending), dao.observe().first())
                assertEquals(listOf(old), dao.observeStories().first())
                dao.replaceLibrary(emptyList())
                assertEquals(listOf(pending), dao.observe().first())
                assertTrue(dao.observeStories().first().isEmpty())
            } finally {
                database.close()
            }
        }

    @Test fun completionRollbackKeepsPendingRowAndLeavesNoPartialStory() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            val database = Room.inMemoryDatabaseBuilder<DozyDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
            try {
                val dao = database.generations()
                val pending = GenerationEntity("pending", "configuration", jobId = "job", status = "CREATING_COVER")
                val story = StoryEntity("story", jobId = "job", requestKey = "pending", result = "complete")
                dao.upsert(pending)
                dao.replaceLibrary(listOf(StoryEntity("story", summary = "preview")))
                assertEquals(pending, dao.find("pending"))

                try {
                    database.withWriteTransaction {
                        dao.completeGeneration("pending", story)
                        throw CancellationException("cancel transaction")
                    }
                    fail("Cancellation must propagate")
                } catch (_: CancellationException) {
                }
                assertEquals(pending, dao.find("pending"))
                assertNull(dao.findStory("story")?.result)

                dao.completeGeneration("pending", story)
                assertNull(dao.find("pending"))
                assertEquals(story, dao.findStory("story"))
            } finally {
                database.close()
            }
        }

    @Test fun remoteSummariesSurviveRestartWithoutDownloadingFullStories() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("library-test.db")

            fun open() = Room.databaseBuilder<DozyDatabase>(context, "library-test.db").setDriver(AndroidSQLiteDriver()).build()
            val progress = ProgressApi()
            val api =
                object : GenerationApi by progress {
                    override suspend fun stories(cursor: String?) =
                        StoryPageDto(
                            listOf(
                                StorySummaryDto("story", "A star", "https://example.test/cover.png", 1, 100),
                            ),
                        )
                }
            var database = open()
            try {
                val repo = testGenerationRepository(database.generations(), api)
                assertTrue(repo.refreshStories())
                assertEquals(0, progress.storyReads)
                database.close()
                database = open()
                val restored = testGenerationRepository(database.generations(), api)
                assertEquals(
                    "story",
                    restored
                        .summaries()
                        .first()
                        .single()
                        .id.value,
                )
                assertEquals("Once upon a time", restored.readStory(Story.Id("story"))?.text)
                assertEquals(1, progress.storyReads)
                assertEquals(1, restored.summaries().first().size)
                database.close()
                database = open()
                progress.storyUnavailable = true
                assertEquals(
                    "Once upon a time",
                    testGenerationRepository(database.generations(), api).readStory(Story.Id("story"))?.text,
                )
                assertEquals(1, progress.storyReads)
            } finally {
                database.close()
                context.deleteDatabase("library-test.db")
            }
        }

    @Test fun queuedStatusAndReadyResultSurviveClosingAndReopeningDatabase() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("generation-test.db")

            fun open() = Room.databaseBuilder<DozyDatabase>(context, "generation-test.db").setDriver(AndroidSQLiteDriver()).build()
            val api = ProgressApi()
            val id = GenerationJob.Id("job")
            var database = open()
            try {
                testGenerationRepository(database.generations(), api).start(GenerationJob.RequestKey("key"), api.configuration)
                database.close()
                database = open()
                val repository = testGenerationRepository(database.generations(), api)
                assertEquals(
                    id,
                    repository
                        .observe()
                        .first()
                        .single()
                        .id,
                )
                api.remote = api.ready()
                repository.refresh(id)
                assertNull(database.generations().find("key"))
                assertTrue(database.generations().findStory("story")?.result != null)
                database.close()
                database = open()
                val restored = testGenerationRepository(database.generations(), api)
                repeat(2) { restored.refresh(id) }
                assertEquals(
                    "Once upon a time",
                    restored
                        .stories()
                        .first()
                        .single()
                        .text,
                )
                assertEquals(
                    Story.Id("story"),
                    restored
                        .observe()
                        .first()
                        .single()
                        .storyId,
                )
                assertEquals(1, api.starts)
                assertEquals(0, api.storyReads)
            } finally {
                database.close()
                context.deleteDatabase("generation-test.db")
            }
        }

    @Test fun permissionDenialIsNormalAndNotificationPendingIntentTargetsSpecificStory() =
        runBlocking {
            val application: Application = RuntimeEnvironment.getApplication()
            val notifier = AndroidStoryCompletionNotifier(application)
            shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            assertFalse(notifier.deliver(Story.Id("story")))
            shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            assertTrue(notifier.deliver(Story.Id("story")))
            val manager = application.getSystemService(NotificationManager::class.java)
            val first = shadowOf(manager).allNotifications.single()
            val intent = shadowOf(first.contentIntent).savedIntent
            assertEquals(MainActivity::class.java.name, intent.component?.className)
            assertEquals("story", notificationStoryId(intent.dataString))
            // A second completion has a different PendingIntent identity, including from a cold launch.
            assertTrue(notifier.deliver(Story.Id("other")))
            val ids = shadowOf(manager).allNotifications.map { notificationStoryId(shadowOf(it.contentIntent).savedIntent.dataString) }
            assertEquals(setOf("story", "other"), ids.toSet())
            notifier.deliver(Story.Id("story"))
            assertEquals(2, shadowOf(manager).allNotifications.size)
        }
}
