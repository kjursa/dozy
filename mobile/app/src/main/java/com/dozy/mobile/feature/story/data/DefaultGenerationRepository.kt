package com.dozy.mobile.feature.story.data

import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.data.database.*
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Persist before sending so uncertain responses can safely replay the exact same snapshots. */
class DefaultGenerationRepository(
    private val dao: GenerationDao,
    private val api: GenerationApi,
) : GenerationRepository {
    private val mutex = Mutex()

    override suspend fun configuration(key: GenerationJob.RequestKey): GenerationConfiguration? =
        dao.find(key.value)?.let { Json.decodeFromString<GenerationRequestDto>(it.configuration).toDomain() }

    override suspend fun accepted(key: GenerationJob.RequestKey) = dao.find(key.value)?.toJob()

    override suspend fun start(
        key: GenerationJob.RequestKey,
        configuration: GenerationConfiguration,
    ): GenerationJob? =
        mutex.withLock {
            val row = dao.find(key.value) ?: GenerationEntity(key.value, Json.encodeToString(configuration.toDto())).also { dao.upsert(it) }
            row.toJob()?.let { return@withLock it }
            val response = api.start(key.value, Json.decodeFromString<GenerationRequestDto>(row.configuration)) ?: return@withLock null
            val accepted = row.copy(jobId = response.jobId, status = response.status)
            dao.upsert(accepted)
            accepted.toJob()
        }
}
