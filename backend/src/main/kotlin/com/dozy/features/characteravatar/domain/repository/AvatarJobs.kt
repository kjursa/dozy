package com.dozy.features.characteravatar.domain.repository

import com.dozy.domain.id.AssetId
import com.dozy.domain.id.AvatarJobId
import com.dozy.domain.id.OwnerId
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.model.AvatarJob

interface AvatarJobs {
    suspend fun create(owner: OwnerId, input: AvatarInput): AvatarJob
    suspend fun find(owner: OwnerId, id: AvatarJobId): AvatarJob?
    suspend fun recover()
    suspend fun claim(): AvatarJob?
    suspend fun complete(job: AvatarJob, success: Boolean)
}
fun interface AvatarGenerator { suspend fun generate(input: AvatarInput): ByteArray }
interface AvatarStorage {
    suspend fun put(id: AssetId, bytes: ByteArray)
    suspend fun get(id: AssetId): ByteArray?
}
