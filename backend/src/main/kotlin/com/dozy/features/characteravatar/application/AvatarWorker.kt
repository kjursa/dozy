package com.dozy.features.characteravatar.application

import com.dozy.config.AvatarConfig
import com.dozy.features.characteravatar.domain.repository.AvatarGenerator
import com.dozy.features.characteravatar.domain.repository.AvatarJobs
import com.dozy.features.characteravatar.domain.repository.AvatarStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AvatarWorker(
    private val jobs: AvatarJobs,
    private val generator: AvatarGenerator,
    private val storage: AvatarStorage,
    private val config: AvatarConfig,
) {
    suspend fun processNext(): Boolean {
        val job = jobs.claim() ?: return false
        val success = try {
            storage.put(job.assetId, generator.generate(job.input))
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        jobs.complete(job, success)
        return true
    }

    fun start(scope: CoroutineScope) = scope.launch {
        jobs.recover()
        repeat(config.concurrency) {
            launch {
                while (true) {
                    try {
                        if (!processNext()) delay(100)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        delay(1000)
                    }
                }
            }
        }
    }
}
