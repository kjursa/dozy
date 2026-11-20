package com.dozy.features.place.data.database

import com.dozy.domain.id.OwnerId
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.SavePlace
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

internal fun JdbcTransaction.visiblePlaces(ownerId: OwnerId): List<Place> =
    Places
        .selectAll()
        .where {
            (Places.ownerId eq ownerId.value) or (Places.origin eq Place.Origin.SYSTEM)
        }.map { it.toPlace() }

internal fun JdbcTransaction.existingPlace(id: PlaceId): ExistingPlace? =
    Places
        .selectAll()
        .where { Places.id eq id.value }
        .singleOrNull()
        ?.let {
            ExistingPlace(
                ownerId = it[Places.ownerId]?.let { value -> requireNotNull(OwnerId.parse(value)) },
                origin = it[Places.origin],
            )
        }

internal fun JdbcTransaction.insertPlace(
    ownerId: OwnerId,
    id: PlaceId,
    command: SavePlace,
) {
    Places.insert {
        it[Places.id] = id.value
        it[Places.ownerId] = ownerId.value
        it[origin] = Place.Origin.USER
        it.setFields(command)
    }
}

internal fun JdbcTransaction.updatePlace(
    id: PlaceId,
    command: SavePlace,
) {
    Places.update({ Places.id eq id.value }) { it.setFields(command) }
}

private fun UpdateBuilder<*>.setFields(command: SavePlace) {
    this[Places.name] = command.name.value
    this[Places.description] = command.description.value
    this[Places.inhabitants] = command.inhabitants?.value
    this[Places.updatedAt] = CustomFunction("datetime", TextColumnType(), stringLiteral("now"))
}

internal data class ExistingPlace(
    val ownerId: OwnerId?,
    val origin: Place.Origin,
)
