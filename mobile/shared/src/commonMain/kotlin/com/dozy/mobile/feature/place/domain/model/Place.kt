package com.dozy.mobile.feature.place.domain.model

import kotlin.jvm.JvmInline

data class Place(
    val id: Id,
    val name: Name,
    val description: Description,
    val inhabitants: Inhabitants? = null,
    val ownership: Ownership,
    val editable: Boolean,
) {
    val canEdit: Boolean get() = ownership == Ownership.CUSTOM && editable

    @JvmInline value class Id(
        val value: String,
    )

    @JvmInline value class Name(
        val value: String,
    )

    @JvmInline value class Description(
        val value: String,
    )

    @JvmInline value class Inhabitants(
        val value: String,
    )

    enum class Ownership { CUSTOM, PREDEFINED }
}
