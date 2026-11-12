package com.dozy.mobile.feature.place.domain.model

class PlaceInput private constructor(
    val name: Place.Name,
    val description: Place.Description,
    val inhabitants: Place.Inhabitants?,
) {
    companion object {
        fun validate(
            name: String,
            description: String,
            inhabitants: String,
        ): Validation {
            fun error(
                value: String,
                max: Int,
                required: Boolean,
            ): PlaceErrors.Error? =
                when {
                    required && value.isBlank() -> PlaceErrors.Error.REQUIRED
                    value.length > max -> PlaceErrors.Error.TOO_LONG
                    else -> null
                }
            val n = name.trim()
            val d = description.trim()
            val i = inhabitants.trim()
            val errors = PlaceErrors(error(n, 80, true), error(d, 500, true), error(i, 500, false))
            return if (errors == PlaceErrors()) {
                Validation.Valid(PlaceInput(Place.Name(n), Place.Description(d), i.takeIf { it.isNotEmpty() }?.let(Place::Inhabitants)))
            } else {
                Validation.Invalid(errors)
            }
        }
    }

    sealed interface Validation {
        data class Valid(
            val input: PlaceInput,
        ) : Validation

        data class Invalid(
            val errors: PlaceErrors,
        ) : Validation
    }
}

data class PlaceErrors(
    val name: Error? = null,
    val description: Error? = null,
    val inhabitants: Error? = null,
) {
    enum class Error { REQUIRED, TOO_LONG }
}
