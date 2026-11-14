package com.dozy.features.place.domain.model

data class Place(
    val id: Id,
    val name: Name,
    val description: Description,
    val inhabitants: Inhabitants?,
    val ownership: Ownership,
) {
    @JvmInline
    value class Id private constructor(val value: String) {
        sealed interface Error {
            data object Empty : Error
        }

        companion object {
            fun parse(raw: String): ParseResult<Id, Error> =
                if (raw.isBlank()) ParseResult.Invalid(Error.Empty)
                else ParseResult.Valid(Id(raw))

            internal fun fromDatabase(value: String): Id = Id(value)
        }

    }

    @JvmInline
    value class Name private constructor(val value: String) {
        sealed interface Error {
            data object Empty : Error
            data class TooLong(val max: Int) : Error
        }

        companion object {
            fun parse(raw: String): ParseResult<Name, Error> {
                val normalized = raw.trim()
                return when {
                    normalized.isEmpty() -> ParseResult.Invalid(Error.Empty)
                    normalized.length > 80 -> ParseResult.Invalid(Error.TooLong(80))
                    else -> ParseResult.Valid(Name(normalized))
                }
            }

            internal fun fromDatabase(value: String): Name = Name(value)
        }

    }

    @JvmInline
    value class Description private constructor(val value: String) {
        sealed interface Error {
            data object Empty : Error
            data class TooLong(val max: Int) : Error
        }

        companion object {
            fun parse(raw: String): ParseResult<Description, Error> {
                val normalized = raw.trim()
                return when {
                    normalized.isEmpty() -> ParseResult.Invalid(Error.Empty)
                    normalized.length > 500 -> ParseResult.Invalid(Error.TooLong(500))
                    else -> ParseResult.Valid(Description(normalized))
                }
            }

            internal fun fromDatabase(value: String): Description = Description(value)
        }

    }

    @JvmInline
    value class Inhabitants private constructor(val value: String) {
        sealed interface Error {
            data class TooLong(val max: Int) : Error
        }

        companion object {
            fun parse(raw: String): ParseResult<Inhabitants?, Error> {
                val normalized = raw.trim()
                return when {
                    normalized.isEmpty() -> ParseResult.Valid(null)
                    normalized.length > 500 -> ParseResult.Invalid(Error.TooLong(500))
                    else -> ParseResult.Valid(Inhabitants(normalized))
                }
            }

            internal fun fromDatabase(value: String): Inhabitants = Inhabitants(value)
        }

    }

    enum class Ownership { CUSTOM, PREDEFINED }
}

sealed interface ParseResult<out T, out E> {
    data class Valid<T>(val value: T) : ParseResult<T, Nothing>
    data class Invalid<E>(val error: E) : ParseResult<Nothing, E>
}

class SavePlace private constructor(
    val name: Place.Name,
    val description: Place.Description,
    val inhabitants: Place.Inhabitants?,
) {
    sealed interface Error {
        data class Name(val error: Place.Name.Error) : Error
        data class Description(val error: Place.Description.Error) : Error
        data class Inhabitants(val error: Place.Inhabitants.Error) : Error
    }

    companion object {
        fun parse(name: String, description: String, inhabitants: String?): ParseResult<SavePlace, Error> {
            val parsedName = when (val result = Place.Name.parse(name)) {
                is ParseResult.Valid -> result.value
                is ParseResult.Invalid -> return ParseResult.Invalid(Error.Name(result.error))
            }
            val parsedDescription = when (val result = Place.Description.parse(description)) {
                is ParseResult.Valid -> result.value
                is ParseResult.Invalid -> return ParseResult.Invalid(Error.Description(result.error))
            }
            val parsedInhabitants = when (val result = inhabitants?.let(Place.Inhabitants::parse)
                ?: ParseResult.Valid(null)) {
                is ParseResult.Valid -> result.value
                is ParseResult.Invalid -> return ParseResult.Invalid(Error.Inhabitants(result.error))
            }
            return ParseResult.Valid(SavePlace(parsedName, parsedDescription, parsedInhabitants))
        }
    }
}

sealed interface SavePlaceResult {
    data object Created : SavePlaceResult
    data object Updated : SavePlaceResult
    data object Forbidden : SavePlaceResult
    data object Conflict : SavePlaceResult
}
