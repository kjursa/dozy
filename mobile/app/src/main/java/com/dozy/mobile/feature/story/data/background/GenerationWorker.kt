package com.dozy.mobile.feature.story.data.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dozy.mobile.feature.story.domain.usecase.ReconcileGenerations
import kotlinx.coroutines.delay

class GenerationWorker(
    context: Context,
    parameters: WorkerParameters,
    private val reconcile: ReconcileGenerations,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        // Stay within WorkManager's execution window, then hand continuation back to the OS.
        repeat(48) {
            if (reconcile() == ReconcileGenerations.Result.Settled) return Result.success()
            delay(10_000)
        }
        return Result.retry()
    }
}
