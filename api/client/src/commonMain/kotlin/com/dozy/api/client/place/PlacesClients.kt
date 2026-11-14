package com.dozy.api.client.place

import com.dozy.api.client.place.v1.PlacesClient as PlacesClientV1

interface PlacesClients {
    val v1: PlacesClientV1
}

internal class DefaultPlacesClients(
    override val v1: PlacesClientV1,
) : PlacesClients
