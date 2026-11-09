package com.dozy.mobile.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JvmInline value class CharacterId(
    val value: String,
)

enum class CharacterValueError { EMPTY, TOO_LONG, LINE_BREAK }

class CharacterValueException(
    val reason: CharacterValueError,
) : IllegalArgumentException(reason.name)

@JvmInline value class CharacterName private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String): CharacterName {
            val value = validatedText(raw, 50)
            if ('\n' in value || '\r' in value) throw CharacterValueException(CharacterValueError.LINE_BREAK)
            return CharacterName(value)
        }
    }
}

@JvmInline value class CharacterPersonality private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String) = CharacterPersonality(validatedText(raw, 2000))
    }
}

@JvmInline value class CharacterAppearance private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String) = CharacterAppearance(validatedText(raw, 2000))
    }
}

private fun validatedText(
    raw: String,
    limit: Int,
): String {
    val value = raw.trim()
    if (value.isEmpty()) throw CharacterValueException(CharacterValueError.EMPTY)
    if (value.length > limit) throw CharacterValueException(CharacterValueError.TOO_LONG)
    return value
}

enum class CharacterType { CHILD, ANIMAL, FANTASY, OTHER }

data class CharacterDraft(
    val name: CharacterName,
    val personality: CharacterPersonality,
    val appearance: CharacterAppearance,
    val type: CharacterType,
)

data class Character(
    val id: CharacterId,
    val details: CharacterDraft,
)

enum class CharacterField { NAME, PERSONALITY, APPEARANCE, TYPE }

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
    data class Success(
        val saved: SavedCharacter,
    ) : SaveCharacterResult

    data object Failure : SaveCharacterResult
}

class SaveCharacter(
    private val repository: CharacterRepository,
    private val ids: CharacterIdGenerator,
) {
    suspend operator fun invoke(draft: CharacterDraft): SaveCharacterResult =
        try {
            SaveCharacterResult.Success(repository.save(Character(ids.next(), draft)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SaveCharacterResult.Failure
        }
}

/** Both lists share the same durable source; Room emits again when a character is saved. */
class ObserveCharacters(
    private val repository: CharacterRepository,
) {
    operator fun invoke(): Flow<CharacterCollection> = repository.observe().map { CharacterCollection(it) }
}
