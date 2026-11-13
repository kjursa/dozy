package com.dozy.features.place.data.database

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.PlaceInput
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

internal fun JdbcTransaction.visiblePlaces(installation: InstallationId): List<Place> =
    Places.selectAll().where {
        (Places.installationId eq installation.value) or (Places.ownership eq Place.Ownership.PREDEFINED)
    }.map { it.toPlace() }

internal fun JdbcTransaction.existingPlace(id: Place.Id): ExistingPlace? =
    Places.selectAll().where { Places.id eq id.value }
        .singleOrNull()?.let { ExistingPlace(it[Places.installationId]) }

internal fun JdbcTransaction.upsertPlace(installation: InstallationId, id: Place.Id, input: PlaceInput) {
    Places.upsert(Places.id, onUpdateExclude = listOf(Places.installationId, Places.ownership)) {
        it[Places.id] = id.value
        it[installationId] = installation.value
        it[name] = input.name
        it[description] = input.description
        it[inhabitants] = input.inhabitants
        it[ownership] = Place.Ownership.CUSTOM
        it[updatedAt] = CustomFunction("datetime", TextColumnType(), stringLiteral("now"))
    }
}

internal data class ExistingPlace(val installationId: String?)
