package com.dozy.mobile.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JvmInline value class CharacterId(
    val value: String,
)

@JvmInline value class CharacterName(
    val value: String,
)

@JvmInline value class CharacterPersonality(
    val value: String,
)

@JvmInline value class CharacterAppearance(
    val value: String,
)

enum class CharacterType { CHILD, ANIMAL, FANTASY, OTHER }

data class CharacterDraft(
    val name: CharacterName,
    val personality: CharacterPersonality,
    val appearance: CharacterAppearance,
    val type: CharacterType?,
)

data class Character(
    val id: CharacterId,
    val details: CharacterDraft,
)

enum class CharacterField { NAME, PERSONALITY, APPEARANCE, TYPE }

data class CharacterValidation(
    val errors: Map<CharacterField, String>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

class CharacterValidator {
    fun validate(draft: CharacterDraft): CharacterValidation =
        CharacterValidation(
            buildMap {
                checkText(CharacterField.NAME, draft.name.value, 80)
                checkText(CharacterField.PERSONALITY, draft.personality.value, 2000)
                checkText(CharacterField.APPEARANCE, draft.appearance.value, 2000)
                if (draft.type == null) put(CharacterField.TYPE, "Choose a character type.")
            },
        )

    private fun MutableMap<CharacterField, String>.checkText(
        field: CharacterField,
        text: String,
        limit: Int,
    ) {
        if (text.isBlank()) {
            put(field, "This field is required.")
        } else if (text.trim().length > limit) {
            put(field, "Use at most $limit characters.")
        }
    }
}

enum class CharacterSync { SYNCED, PENDING }

data class SavedCharacter(
    val character: Character,
    val sync: CharacterSync,
)

data class CharacterCollection(
    val characters: List<Character>,
)

interface CharacterRepository {
    suspend fun save(character: Character): SavedCharacter

    fun observe(): Flow<List<Character>>
}

fun interface CharacterIdGenerator {
    fun next(): CharacterId
}

sealed interface SaveCharacterResult {
    data class Invalid(
        val validation: CharacterValidation,
    ) : SaveCharacterResult

    data class Success(
        val saved: SavedCharacter,
    ) : SaveCharacterResult

    data object Failure : SaveCharacterResult
}

class SaveCharacter(
    private val repository: CharacterRepository,
    private val validator: CharacterValidator,
    private val ids: CharacterIdGenerator,
) {
    suspend operator fun invoke(draft: CharacterDraft): SaveCharacterResult {
        val validation = validator.validate(draft)
        if (!validation.isValid) return SaveCharacterResult.Invalid(validation)
        val normalized =
            draft.copy(
                name = CharacterName(draft.name.value.trim()),
                personality = CharacterPersonality(draft.personality.value.trim()),
                appearance = CharacterAppearance(draft.appearance.value.trim()),
            )
        return try {
            SaveCharacterResult.Success(repository.save(Character(ids.next(), normalized)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SaveCharacterResult.Failure
        }
    }
}

/** Both lists share the same durable source; Room emits again when a character is saved. */
class ObserveCharacters(
    private val repository: CharacterRepository,
) {
    operator fun invoke(): Flow<CharacterCollection> = repository.observe().map { CharacterCollection(it) }
}
