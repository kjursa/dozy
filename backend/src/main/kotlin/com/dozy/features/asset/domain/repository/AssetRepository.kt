package com.dozy.features.asset.domain.repository

import com.dozy.domain.id.AssetId
import com.dozy.domain.id.OwnerId


fun interface AssetRepository {
    suspend fun read(installation: OwnerId, id: AssetId): ByteArray?
}
