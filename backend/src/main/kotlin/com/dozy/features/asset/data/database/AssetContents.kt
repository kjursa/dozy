package com.dozy.features.asset.data.database

import org.jetbrains.exposed.v1.core.Table

internal object AssetContents : Table("asset_contents") {
    val assetId = text("asset_id")
    val bytes = binary("bytes")
    override val primaryKey = PrimaryKey(assetId)
}
