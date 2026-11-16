package com.dozy.features.place.api.v1

import com.dozy.api.contracts.v1.place.PlacesSaveError
import io.ktor.http.HttpStatusCode

class PlaceApiException(val status: HttpStatusCode, val error: PlacesSaveError) : RuntimeException()
