package com.dozy.mobile.feature.story.data.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/** Enforce omitted hidden fields independently of the shared HTTP client's JSON defaults. */
internal fun GenerationRequestDto.toRequestBody(): JsonObject {
    val fields = Json.encodeToJsonElement(this).jsonObject
    return JsonObject(
        fields.filter { (name, value) ->
            value != JsonNull && (mode != "AUTOMATIC" || name in setOf("cast", "mode", "length"))
        },
    )
}
