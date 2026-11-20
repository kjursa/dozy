package com.dozy.features.place.api.v1

import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.domain.id.PlaceId
import com.dozy.features.place.domain.model.Place

fun Place.toResponse(assetBaseUrl: String) = PlaceResponse(
    id.value, name.value, description.value, origin.name, origin == Place.Origin.USER, inhabitants?.value,
    id.imageUrl(assetBaseUrl),
)

internal fun PlaceId.imageUrl(assetBaseUrl: String): String {
    val icon = if (value == "builtin:woods") "forest" else "cottage"
    return "${assetBaseUrl.trimEnd('/')}${ApiVersion.V1.path}/place-icons/$icon.png"
}
