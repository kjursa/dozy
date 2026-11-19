package com.dozy.features.story.domain.model

import com.dozy.domain.id.AssetId
import com.dozy.domain.id.CharacterId
import com.dozy.domain.id.GenerationId
import com.dozy.domain.id.JobId
import com.dozy.domain.id.LeaseToken
import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.domain.id.StoryId

/** Snapshots are historical data: current character/place creation limits do not apply. */
data class HeroSnapshot(val id: CharacterId, val name: String, val personality: String, val appearance: String, val type: String, val avatar: AvatarSnapshot?)
data class AvatarSnapshot(val id: AssetId, val url: String)
data class PlaceSnapshot(val id: PlaceId, val name: String, val description: String, val origin: String, val editable: Boolean, val inhabitants: String?)
data class StoryConfiguration(val cast: List<HeroSnapshot>, val mode: Mode, val length: Length, val place: PlaceSnapshot?, val idea: String?, val ending: String?) {
    enum class Mode { MANUAL, AUTOMATIC }
    enum class Length { SHORT, LONG }
}
class StartGeneration private constructor(val configuration: StoryConfiguration) {
    enum class Field { CAST, IDEA, ENDING }
    enum class Reason { REQUIRED, INVALID_VALUE, TOO_LONG }
    sealed interface Result {
        data class Valid(val command: StartGeneration) : Result
        data class Invalid(val field: Field, val reason: Reason) : Result
    }
    companion object {
        fun parse(configuration: StoryConfiguration): Result {
            if (configuration.cast.isEmpty()) return Result.Invalid(Field.CAST, Reason.REQUIRED)
            if (configuration.cast.distinctBy { it.id }.size != configuration.cast.size) return Result.Invalid(Field.CAST, Reason.INVALID_VALUE)
            val idea = configuration.idea?.trim()
            val ending = configuration.ending?.trim()
            if (idea != null && idea.length > 1000) return Result.Invalid(Field.IDEA, Reason.TOO_LONG)
            if (ending != null && ending.length > 500) return Result.Invalid(Field.ENDING, Reason.TOO_LONG)
            return Result.Valid(StartGeneration(configuration.copy(idea = idea, ending = ending)))
        }
    }
}
enum class Stage { QUEUED, CREATING_TEXT, CREATING_COVER, READY, FAILED }
enum class Failure { CONTENT_REJECTED, GENERATION_FAILED }
data class Job(val id: JobId, val stage: Stage, val storyId: StoryId? = null, val failure: Failure? = null)
data class SavedStory(val id: StoryId, val title: String, val text: String, val coverUrl: String, val configuration: StoryConfiguration, val createdAt: Long)
data class Execution(
    val job: Job,
    val ownerId: OwnerId,
    val token: LeaseToken,
    val configuration: StoryConfiguration,
    val title: String?,
    val text: String?,
    val ordinal: Int,
    val id: GenerationId,
)
sealed interface RetrySubmission {
    data class Accepted(val job: Job) : RetrySubmission
    data object JobNotFound : RetrySubmission
    data object JobNotFailed : RetrySubmission
    data object ActiveJobExists : RetrySubmission
    data object IdempotencyConflict : RetrySubmission
}
sealed interface Submission {
    data class Accepted(val job: Job) : Submission
    data object IdempotencyConflict : Submission
    data object ActiveJobExists : Submission
    data object ReferenceUnavailable : Submission
}
data class GeneratedText(val title: String, val text: String)
class ContentRejectedException : Exception()
fun interface TextGenerator { suspend fun generate(configuration: StoryConfiguration): GeneratedText }
fun interface CoverGenerator { suspend fun generate(configuration: StoryConfiguration): ByteArray }
