package com.dozy.di

import com.dozy.config.AppConfig
import com.dozy.config.DatabaseConfig
import com.dozy.features.place.application.PlaceService
import com.dozy.features.place.data.repository.SqlitePlaceRepository
import com.dozy.features.place.domain.repository.PlaceRepository
import com.dozy.infrastructure.database.SqliteDatabase
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

fun Application.configureDependencies() {
    val configuration = environment.config
    dependencies {
        provide<AppConfig> { AppConfig.from(configuration) }
        provide<DatabaseConfig> { resolve<AppConfig>().database }
        provide<SqliteDatabase> { SqliteDatabase(resolve()) }
        provide<PlaceRepository> { SqlitePlaceRepository(resolve()) }
        provide<PlaceService> { PlaceService(resolve()) }
    }
}
