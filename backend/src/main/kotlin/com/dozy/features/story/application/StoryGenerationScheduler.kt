package com.dozy.features.story.application

import com.dozy.config.GenerationConfig
import com.dozy.features.story.domain.repository.GenerationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

class StoryGenerationScheduler(
    private val repository: GenerationRepository,
    private val worker: GenerationWorker,
    private val config: GenerationConfig,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) =
        scope.launch {
            val completed = Channel<Unit>(config.concurrency)
            var active = 0

            while (isActive) {
                if (active == config.concurrency) {
                    completed.receive()
                    active--
                    continue
                }

                val execution =
                    try {
                        repository.claim()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        delay(250)
                        continue
                    }

                if (execution == null) {
                    // Wake on a freed slot or check again for newly queued work and expired leases.
                    val slotFreed =
                        select {
                            completed.onReceive { true }
                            onTimeout(5_000) { false }
                        }
                    if (slotFreed) active--
                    continue
                }

                active++
                launch {
                    try {
                        worker.run(execution)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Storage failures leave the current stage recoverable when the lease expires.
                    } finally {
                        completed.trySend(Unit)
                    }
                }
            }
        }
}
