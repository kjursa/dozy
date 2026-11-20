package com.dozy.features.place.api.v1

import com.dozy.api.contracts.ApiVersion
import com.dozy.api.contracts.v1.place.PlaceResponse
import com.dozy.features.place.domain.model.Place

fun Place.toResponse(assetBaseUrl: String) =
    PlaceResponse(
        id.value,
        name.value,
        description.value,
        origin.name,
        origin == Place.Origin.USER,
        inhabitants?.value,
        thumbnail.imageUrl(assetBaseUrl),
    )

internal fun Place.Thumbnail.imageUrl(assetBaseUrl: String): String =
    "${assetBaseUrl.trimEnd('/')}${ApiVersion.V1.path}/place-icons/$fileName"
