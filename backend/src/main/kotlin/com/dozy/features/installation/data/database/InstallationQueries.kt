package com.dozy.features.installation.data.database

import com.dozy.domain.id.OwnerId
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertIgnore

internal fun JdbcTransaction.registerInstallation(installation: OwnerId) {
    Installations.insertIgnore { it[id] = installation.value }
}
