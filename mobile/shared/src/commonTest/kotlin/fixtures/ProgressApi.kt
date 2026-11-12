package com.dozy.mobile
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.data.toDto
import com.dozy.mobile.feature.story.domain.model.*
import kotlinx.coroutines.CompletableDeferred

internal class ProgressApi : GenerationApi {
    val configuration = GenerationConfiguration(listOf(character()), GenerationJob.Mode.MANUAL, Story.Length.SHORT, null, "A star", "Home")
    var remote = GenerationStatusDto("job", "QUEUED")
    var offline = false
    var storyUnavailable = false
    var loseRetryResponse = false
    var retryGate: CompletableDeferred<Unit>? = null
    var starts = 0
    var reads = 0
    var storyReads = 0
    val retryKeys = mutableListOf<String>()

    override suspend fun start(
        key: String,
        request: GenerationRequestDto,
    ): GenerationAcceptedDto {
        starts++
        return GenerationAcceptedDto("job", "QUEUED")
    }

    override suspend fun status(id: String): GenerationStatusDto {
        reads++
        if (offline) error("Offline")
        return remote
    }

    override suspend fun story(id: String): StoryDto? {
        storyReads++
        if (storyUnavailable) return null
        return StoryDto("story", "A star", "Once upon a time", "https://example.test/cover.png", configuration.toDto())
    }

    override suspend fun retry(
        id: String,
        key: String,
    ): GenerationAcceptedDto {
        retryKeys += key
        retryGate?.await()
        if (loseRetryResponse) error("Response lost")
        remote = GenerationStatusDto("job", "QUEUED")
        return GenerationAcceptedDto("job", "QUEUED")
    }
}
