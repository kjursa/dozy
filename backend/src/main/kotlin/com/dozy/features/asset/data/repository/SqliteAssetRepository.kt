package com.dozy.features.asset.data.repository

import com.dozy.domain.id.AssetId
import com.dozy.domain.id.OwnerId
import com.dozy.features.asset.data.database.AssetContents
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.features.characteravatar.domain.repository.AvatarStorage
import com.dozy.infrastructure.database.SqliteDatabase
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class SqliteAssetRepository(
    private val database: SqliteDatabase,
    private val avatarStorage: AvatarStorage? = null,
) : AssetRepository {
    override suspend fun read(installation: OwnerId, id: AssetId): ByteArray? {
        val asset = database.write {
            if (!Assets.selectAll().where {
                    (Assets.id eq id.value) and (Assets.installationId eq installation.value) and (Assets.status eq "READY")
                }.any()
            ) return@write null
            OwnedAsset(AssetContents.selectAll().where { AssetContents.assetId eq id.value }.singleOrNull()?.get(AssetContents.bytes))
        } ?: return null
        return asset.bytes ?: avatarStorage?.get(id)
    }

    private class OwnedAsset(val bytes: ByteArray?)
}
