package com.dozy.mobile.feature.place.domain.model

import kotlin.jvm.JvmInline

data class Place(
    val id: Id,
    val name: Name,
    val description: Description,
    val inhabitants: Inhabitants? = null,
    val origin: Origin,
    val editable: Boolean,
) {
    val canEdit: Boolean get() = origin == Origin.USER && editable

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

    enum class Origin { USER, SYSTEM }
}
