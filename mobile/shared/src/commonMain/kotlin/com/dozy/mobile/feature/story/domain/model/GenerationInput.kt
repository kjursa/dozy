package com.dozy.mobile.feature.story.domain.model

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.place.domain.model.Place

class GenerationInput private constructor(
    val cast: List<Character.Id>,
    val mode: GenerationJob.Mode,
    val length: Story.Length,
    val place: Place.Id?,
    val idea: String?,
    val ending: String?,
) {
    companion object {
        fun from(
            cast: List<String>,
            automatic: Boolean,
            length: Int,
            place: String?,
            idea: String,
            ending: String,
        ): Validation {
            val errors =
                GenerationErrors(
                    cast = if (cast.isEmpty() || cast.any { it.isBlank() }) GenerationErrors.Cast.REQUIRED else null,
                    length = if (length !in 0..1) GenerationErrors.Length.INVALID else null,
                    idea = if (!automatic && idea.trim().length > 1000) GenerationErrors.Text.TOO_LONG else null,
                    ending = if (!automatic && ending.trim().length > 500) GenerationErrors.Text.TOO_LONG else null,
                )
            if (errors != GenerationErrors()) return Validation.Invalid(errors)
            return Validation.Valid(
                GenerationInput(
                    cast.distinct().map(Character::Id),
                    if (automatic) GenerationJob.Mode.AUTOMATIC else GenerationJob.Mode.MANUAL,
                    Story.Length.entries[length],
                    if (automatic) null else place?.takeIf { it.isNotBlank() }?.let(Place::Id),
                    if (automatic) null else idea.trim().ifEmpty { null },
                    if (automatic) null else ending.trim().ifEmpty { null },
                ),
            )
        }
    }

    sealed interface Validation {
        data class Valid(
            val input: GenerationInput,
        ) : Validation

        data class Invalid(
            val errors: GenerationErrors,
        ) : Validation
    }
}

data class GenerationErrors(
    val cast: Cast? = null,
    val length: Length? = null,
    val idea: Text? = null,
    val ending: Text? = null,
) {
    enum class Cast { REQUIRED }

    enum class Length { INVALID }

    enum class Text { TOO_LONG }
}
