package com.dozy.features.asset.data.repository

import com.dozy.features.asset.data.database.AssetContents
import com.dozy.features.asset.data.database.Assets
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.features.installation.domain.model.InstallationId
import com.dozy.infrastructure.database.SqliteDatabase
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class SqliteAssetRepository(private val database: SqliteDatabase) : AssetRepository {
    override suspend fun read(installation: InstallationId, id: String): ByteArray? = database.write {
        if (!Assets.selectAll().where { (Assets.id eq id) and (Assets.installationId eq installation.value) and (Assets.status eq "READY") }.any()) return@write null
        AssetContents.selectAll().where { AssetContents.assetId eq id }.singleOrNull()?.get(AssetContents.bytes)
    }
}
