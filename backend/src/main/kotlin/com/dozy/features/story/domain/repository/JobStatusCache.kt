package com.dozy.features.story.domain.repository

import com.dozy.domain.id.JobId
import com.dozy.domain.id.OwnerId
import com.dozy.features.story.domain.model.Job

/** Best-effort cache of statuses from the generation repository. */
interface JobStatusCache {
    suspend fun get(
        ownerId: OwnerId,
        jobId: JobId,
    ): Job?

    suspend fun put(
        ownerId: OwnerId,
        job: Job,
    )

    suspend fun invalidate(
        ownerId: OwnerId,
        jobId: JobId,
    )
}
