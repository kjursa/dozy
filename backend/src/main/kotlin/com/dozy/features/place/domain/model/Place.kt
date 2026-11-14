package com.dozy.features.place.domain.model

data class Place(
    val id: Id,
    val name: Name,
    val description: Description,
    val inhabitants: Inhabitants?,
    val ownership: Ownership,
) {
    @JvmInline value class Id(val value: String)
    @JvmInline value class Name(val value: String)
    @JvmInline value class Description(val value: String)
    @JvmInline value class Inhabitants(val value: String)
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
        sealed interface Name : Error {
            data object Required : Name
            data class TooLong(val maxLength: Int) : Name
        }
        sealed interface Description : Error {
            data object Required : Description
            data class TooLong(val maxLength: Int) : Description
        }
        sealed interface Inhabitants : Error {
            data class TooLong(val maxLength: Int) : Inhabitants
        }
    }

    companion object {
        fun parse(name: String, description: String, inhabitants: String?): ParseResult<SavePlace, Error> {
            val normalizedName = name.trim()
            val parsedName = when {
                normalizedName.isEmpty() -> return ParseResult.Invalid(Error.Name.Required)
                normalizedName.length > 80 -> return ParseResult.Invalid(Error.Name.TooLong(80))
                else -> Place.Name(normalizedName)
            }
            val normalizedDescription = description.trim()
            val parsedDescription = when {
                normalizedDescription.isEmpty() -> return ParseResult.Invalid(Error.Description.Required)
                normalizedDescription.length > 500 -> return ParseResult.Invalid(Error.Description.TooLong(500))
                else -> Place.Description(normalizedDescription)
            }
            val normalizedInhabitants = inhabitants?.trim()?.takeIf { it.isNotEmpty() }
            val parsedInhabitants = when {
                normalizedInhabitants == null -> null
                normalizedInhabitants.length > 500 -> return ParseResult.Invalid(Error.Inhabitants.TooLong(500))
                else -> Place.Inhabitants(normalizedInhabitants)
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
