package com.dozy.mobile

import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.GenerationStatusDto
import com.dozy.mobile.feature.story.data.notification.UnsupportedStoryCompletionNotifier
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.domain.usecase.ReconcileGenerations
import com.dozy.mobile.feature.story.domain.usecase.RetryGeneration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UnsupportedNotificationTest {
    @Test fun unsupportedHostNeverRecordsDeliveryDuringReconciliation() =
        runTest {
            val notifier = UnsupportedStoryCompletionNotifier()
            assertFalse(notifier.deliver(Story.Id("story")))
            val api = ProgressApi()
            val repository = DefaultGenerationRepository(FakeGenerationDao(), api)
            repository.start(GenerationJob.RequestKey("key"), api.configuration)
            api.remote = GenerationStatusDto("job", "READY", "story")
            val reconcile =
                ReconcileGenerations(
                    repository,
                    RetryGeneration(repository, GenerationKeyGenerator { error("No retry expected") }),
                    notifier,
                )
            repeat(2) { assertEquals(ReconcileGenerations.Result.Settled, reconcile()) }
            val job = repository.observe().first().single()
            assertEquals(Story.Id("story"), job.storyId)
            assertFalse(job.notificationDelivered)
        }
}
