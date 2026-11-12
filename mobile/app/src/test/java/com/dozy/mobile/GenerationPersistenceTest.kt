package com.dozy.mobile

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.dozy.mobile.core.data.database.DozyDatabase
import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.GenerationStatusDto
import com.dozy.mobile.feature.story.data.background.AndroidStoryCompletionNotifier
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.presentation.generation.notificationStoryId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GenerationPersistenceTest {
    @org.junit.Before fun initializeSharedResources() {
        initializeComposeResourceProvider()
    }

    @Test fun remoteSummariesSurviveRestartWithoutDownloadingFullStories() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            context.deleteDatabase("library-test.db")

            fun open() = Room.databaseBuilder<DozyDatabase>(context, "library-test.db").setDriver(AndroidSQLiteDriver()).build()
            val progress = ProgressApi()
            val api =
                object : com.dozy.mobile.feature.story.data.api.GenerationApi by progress {
                    override suspend fun stories(cursor: String?) =
                        com.dozy.mobile.feature.story.data.api.StoryPageDto(
                            listOf(
                                com.dozy.mobile.feature.story.data.api
                                    .StorySummaryDto("story", "A star", "https://example.test/cover.png", 1, 100),
                            ),
                        )
                }
            var database = open()
            try {
                val repo = DefaultGenerationRepository(database.generations(), api)
                assertTrue(repo.refreshStories())
                assertEquals(0, progress.storyReads)
                database.close()
                database = open()
                val restored = DefaultGenerationRepository(database.generations(), api)
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
                    DefaultGenerationRepository(database.generations(), api).readStory(Story.Id("story"))?.text,
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
                DefaultGenerationRepository(database.generations(), api).start(GenerationJob.RequestKey("key"), api.configuration)
                database.close()
                database = open()
                val repository = DefaultGenerationRepository(database.generations(), api)
                assertEquals(
                    id,
                    repository
                        .observe()
                        .first()
                        .single()
                        .id,
                )
                api.remote = GenerationStatusDto("job", "READY", "story")
                repository.refresh(id)
                database.close()
                database = open()
                val restored = DefaultGenerationRepository(database.generations(), api)
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
                assertEquals(1, api.storyReads)
            } finally {
                database.close()
                context.deleteDatabase("generation-test.db")
            }
        }

    @Test fun permissionDenialIsNormalAndNotificationPendingIntentTargetsSpecificStory() =
        runBlocking {
            val application: Application = RuntimeEnvironment.getApplication()
            val notifier = AndroidStoryCompletionNotifier(application)
            shadowOf(application).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
            assertFalse(notifier.deliver(Story.Id("story")))
            shadowOf(application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
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
