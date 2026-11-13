package com.dozy.features.installation.data.database

import com.dozy.features.installation.domain.model.InstallationId
import java.sql.Connection

internal fun Connection.registerInstallation(installation: InstallationId) {
    prepareStatement("INSERT INTO installations(id) VALUES (?) ON CONFLICT(id) DO NOTHING").use {
        it.setString(1, installation.value)
        it.executeUpdate()
    }
}
