package com.dozy.features.place.data.database

import com.dozy.domain.VersionedCollection
import com.dozy.domain.id.OwnerId
import com.dozy.features.place.domain.model.Place
import com.dozy.infrastructure.database.SYSTEM_CATALOG_OWNER
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.infrastructure.database.TrackedCollection
import com.dozy.infrastructure.database.collectionVersion

class PlaceDao(
    private val database: SqliteDatabase,
) {
    suspend fun list(
        ownerId: OwnerId,
        isCurrent: (String) -> Boolean,
    ): VersionedCollection<Place> =
        database.write {
            val ownVersion = collectionVersion(TrackedCollection.PLACES, ownerId.value)
            val catalogVersion = collectionVersion(TrackedCollection.PLACES, SYSTEM_CATALOG_OWNER)
            val version = "$ownVersion.$catalogVersion"
            if (isCurrent(version)) return@write VersionedCollection(version, null)
            VersionedCollection(version, visiblePlaces(ownerId))
        }
}
