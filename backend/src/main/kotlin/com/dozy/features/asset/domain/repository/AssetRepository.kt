package com.dozy.features.asset.domain.repository

import com.dozy.features.installation.domain.model.InstallationId

fun interface AssetRepository {
    suspend fun read(installation: InstallationId, id: String): ByteArray?
}
