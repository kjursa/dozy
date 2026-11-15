package com.dozy.features.characteravatar.domain.repository

import com.dozy.features.characteravatar.domain.model.AvatarJob
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.installation.domain.model.InstallationId

interface AvatarJobs {
    suspend fun create(owner: InstallationId, input: AvatarInput): AvatarJob
    suspend fun find(owner: InstallationId, id: String): AvatarJob?
    suspend fun recover()
    suspend fun claim(): AvatarJob?
    suspend fun complete(job: AvatarJob, success: Boolean)
}
fun interface AvatarGenerator { suspend fun generate(input: AvatarInput): ByteArray }
interface AvatarStorage {
    suspend fun put(id: String, bytes: ByteArray)
    suspend fun get(id: String): ByteArray?
}
