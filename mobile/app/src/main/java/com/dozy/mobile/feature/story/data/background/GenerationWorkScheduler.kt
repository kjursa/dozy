package com.dozy.mobile.feature.story.data.background

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class GenerationWorkScheduler(
    private val work: WorkManager,
) {
    fun resume() {
        work.enqueueUniquePeriodicWork(
            "generation-recovery",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<GenerationWorker>(15, TimeUnit.MINUTES).build(),
        )
        checkNow()
    }

    fun checkNow() {
        work.enqueueUniqueWork(
            "generation-status",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<GenerationWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build(),
        )
    }
}

class GenerationWorkerFactory(
    private val reconcile: com.dozy.mobile.feature.story.domain.usecase.ReconcileGenerations,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == GenerationWorker::class.java.name) GenerationWorker(appContext, workerParameters, reconcile) else null
}
