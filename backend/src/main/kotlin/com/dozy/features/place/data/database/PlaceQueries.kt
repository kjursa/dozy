package com.dozy.features.place.data.database

import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.features.place.domain.model.Place
import com.dozy.features.place.domain.model.PlaceInput
import java.sql.Connection
import java.sql.ResultSet

internal fun Connection.visiblePlaces(installation: InstallationId): List<Place> =
    prepareStatement("SELECT * FROM places WHERE installation_id = ? OR ownership = 'PREDEFINED'").use {
        it.setString(1, installation.value)
        it.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.toPlace()) } }
    }

internal fun Connection.existingPlace(id: Place.Id): ExistingPlace? =
    prepareStatement("SELECT installation_id FROM places WHERE id = ?").use {
        it.setString(1, id.value)
        it.executeQuery().use { rows -> if (rows.next()) ExistingPlace(rows.getString(1)) else null }
    }

internal fun Connection.upsertPlace(installation: InstallationId, id: Place.Id, input: PlaceInput) {
    prepareStatement(UPSERT).use {
        it.setString(1, id.value)
        it.setString(2, installation.value)
        it.setString(3, input.name)
        it.setString(4, input.description)
        it.setString(5, input.inhabitants)
        it.executeUpdate()
    }
}

private fun ResultSet.toPlace() = Place(
    Place.Id(getString("id")), getString("name"), getString("description"),
    getString("inhabitants"), Place.Ownership.valueOf(getString("ownership")),
)

internal data class ExistingPlace(val installationId: String?)

private val UPSERT = """
    INSERT INTO places(id, installation_id, name, description, inhabitants, ownership)
    VALUES (?, ?, ?, ?, ?, 'CUSTOM')
    ON CONFLICT(id) DO UPDATE SET
        name = excluded.name, description = excluded.description,
        inhabitants = excluded.inhabitants, updated_at = CURRENT_TIMESTAMP
""".trimIndent()
