package com.dozy.domain.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal object Uuids {
    fun v4(): String = Uuid.generateV4().toString()

    fun v7(): String = Uuid.generateV7().toString()
}
