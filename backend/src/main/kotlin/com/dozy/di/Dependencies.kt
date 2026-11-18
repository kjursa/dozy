package com.dozy.di

import com.dozy.config.AvatarConfig
import com.dozy.features.characteravatar.application.AvatarWorker
import com.dozy.features.characteravatar.data.repository.SqliteAvatarJobs
import com.dozy.features.characteravatar.data.FakeAvatarGenerator
import com.dozy.features.characteravatar.data.LocalAvatarStorage
import com.dozy.features.characteravatar.domain.repository.AvatarJobs
import com.dozy.features.characteravatar.domain.repository.AvatarGenerator
import com.dozy.features.characteravatar.domain.repository.AvatarStorage
import com.dozy.features.character.application.CharacterService
import com.dozy.features.character.data.repository.SqliteCharacterRepository
import com.dozy.features.character.domain.repository.CharacterRepository
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.features.asset.data.repository.SqliteAssetRepository
import com.dozy.config.GenerationConfig
import com.dozy.features.story.application.GenerationWorker
import com.dozy.features.story.application.StoryService
import com.dozy.features.story.data.FakeTextGenerator
import com.dozy.features.story.data.FakeCoverGenerator
import com.dozy.features.story.data.repository.SqliteGenerationRepository
import com.dozy.features.story.domain.model.TextGenerator
import com.dozy.features.story.domain.model.CoverGenerator
import com.dozy.features.story.domain.repository.GenerationRepository
import com.dozy.config.AppConfig
import com.dozy.config.DatabaseConfig
import com.dozy.features.place.application.PlaceService
import com.dozy.features.place.data.repository.SqlitePlaceRepository
import com.dozy.features.place.domain.repository.PlaceRepository
import com.dozy.infrastructure.database.SqliteDatabase
import com.dozy.features.auth.application.AccessTokens
import com.dozy.features.auth.application.AuthService
import com.dozy.config.AuthSecretProvider
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

fun Application.configureDependencies() {
    val configuration = environment.config
    dependencies {
        provide<AuthSecretProvider> { AuthSecretProvider.from(configuration) }
        provide<AccessTokens> { AccessTokens(resolve()) }
        provide<AssetRepository> { SqliteAssetRepository(resolve(), resolve()) }
        provide<GenerationConfig> { GenerationConfig.from(configuration) }
        provide<GenerationRepository> { SqliteGenerationRepository(resolve(), resolve()) }
        provide<TextGenerator> { FakeTextGenerator(resolve()) }
        provide<CoverGenerator> { FakeCoverGenerator(resolve()) }
        provide<StoryService> { StoryService(resolve()) }
        provide<GenerationWorker> { GenerationWorker(resolve(), resolve(), resolve(), resolve()) }
        provide<AppConfig> { AppConfig.from(configuration) }
        provide<AvatarConfig> { resolve<AppConfig>().avatars }
        provide<AvatarJobs> { SqliteAvatarJobs(resolve(), resolve()) }
        provide<AvatarGenerator> { FakeAvatarGenerator(resolve()) }
        provide<AvatarStorage> { LocalAvatarStorage(resolve()) }
        provide<AvatarWorker> { AvatarWorker(resolve(), resolve(), resolve(), resolve()) }
        provide<DatabaseConfig> { resolve<AppConfig>().database }
        provide<SqliteDatabase> { SqliteDatabase(resolve()) }
        provide<AuthService> { AuthService(resolve(), resolve(), resolve()) }
        provide<CharacterRepository> { SqliteCharacterRepository(resolve()) }
        provide<CharacterService> { CharacterService(resolve()) }
        provide<PlaceRepository> { SqlitePlaceRepository(resolve()) }
        provide<PlaceService> { PlaceService(resolve()) }
    }
}
