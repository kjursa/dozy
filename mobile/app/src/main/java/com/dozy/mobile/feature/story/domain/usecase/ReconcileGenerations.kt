package com.dozy.mobile.feature.story.domain.usecase

import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.flow.first

/** Polling belongs to a system scheduler, never to a screen's lifecycle. */
class ReconcileGenerations(
    private val repository: GenerationRepository,
    private val retry: RetryGeneration,
    private val notifier: StoryCompletionNotifier,
) {
    suspend operator fun invoke(): Result =
        runCatchingSuspend {
            var pending = false
            for (saved in repository.observe().first()) {
                if (saved.retryPending) retry(saved.id)
                val current =
                    if (saved.storyId == null &&
                        (saved.status != GenerationJob.Status.FAILED || saved.failure == null || saved.retryPending || saved.statusUnknown)
                    ) {
                        repository.refresh(saved.id) ?: saved
                    } else {
                        saved
                    }
                if (current.status == GenerationJob.Status.READY && !current.notificationDelivered) {
                    if (notifier.deliver(requireNotNull(current.storyId))) repository.notificationDelivered(current.id)
                }
                if (current.statusUnknown ||
                    current.retryPending ||
                    current.status !in setOf(GenerationJob.Status.READY, GenerationJob.Status.FAILED)
                ) {
                    pending = true
                }
            }
            if (pending) Result.Pending else Result.Settled
        }.getOrDefault(Result.Pending)

    sealed interface Result {
        data object Pending : Result

        data object Settled : Result
    }
}

/** Return true only when the system accepts notification delivery. Denial is a normal outcome. */
fun interface StoryCompletionNotifier {
    suspend fun deliver(id: Story.Id): Boolean
}
