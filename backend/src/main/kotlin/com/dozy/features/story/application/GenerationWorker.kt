package com.dozy.features.story.application

import com.dozy.config.GenerationConfig
import com.dozy.features.story.domain.model.ContentRejectedException
import com.dozy.features.story.domain.model.CoverGenerator
import com.dozy.features.story.domain.model.Execution
import com.dozy.features.story.domain.model.Failure
import com.dozy.features.story.domain.model.Stage
import com.dozy.features.story.domain.model.TextGenerator
import com.dozy.features.story.domain.repository.GenerationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** SQLite leases fence stale workers; generators never run inside a transaction. */
class GenerationWorker(
    private val repository: GenerationRepository,
    private val text: TextGenerator,
    private val cover: CoverGenerator,
    private val config: GenerationConfig,
) {
    fun start(scope: CoroutineScope) = List(config.concurrency) {
        scope.launch {
            while (isActive) {
                try {
                    if (!runOne()) delay(100)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Storage/observation failures leave the lease recoverable, never FAILED.
                    delay(250)
                }
            }
        }
    }
    suspend fun runOne(): Boolean {
        val execution = repository.claim() ?: return false
        coroutineScope {
            val heartbeat = launch {
                while (isActive) {
                    delay(config.leaseMillis / 3)
                    if (!repository.renew(execution)) return@launch
                }
            }
            try { execute(execution) } finally { heartbeat.cancel() }
        }
        return true
    }
    private suspend fun execute(execution: Execution) {
        if (execution.job.stage == Stage.CREATING_TEXT) {
            val result = try {
                if (config.textMode == GenerationConfig.TextMode.FAIL_ONCE && execution.ordinal == 1) error("Configured first execution failure")
                text.generate(execution.configuration)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: ContentRejectedException) {
                repository.fail(execution, Failure.CONTENT_REJECTED); return
            } catch (_: Exception) {
                repository.fail(execution, Failure.GENERATION_FAILED); return
            }
            if (!repository.saveText(execution, result)) return
        }
        val bytes = try {
            cover.generate(execution.configuration)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) {
            repository.fail(execution, Failure.GENERATION_FAILED); return
        }
        repository.publish(execution, bytes)
    }
}
