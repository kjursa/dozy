package com.dozy.features.installation.data.database

import com.dozy.features.installation.domain.model.InstallationId
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertIgnore

internal fun JdbcTransaction.registerInstallation(installation: InstallationId) {
    Installations.insertIgnore { it[id] = installation.value }
}
