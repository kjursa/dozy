package com.dozy.mobile.feature.character.di

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.FakeAvatarGenerator
import com.dozy.mobile.feature.character.data.api.CharacterApi
import com.dozy.mobile.feature.character.data.api.KtorCharacterApi
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.DeleteCharacter
import com.dozy.mobile.feature.character.domain.usecase.GenerateCharacterAvatar
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacter
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.RefreshCharacters
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val characterDataModule =
    module {
        single<CharacterApi> { KtorCharacterApi(get(), get()) }
        single<CharacterRepository> {
            val repository = DefaultCharacterRepository(get(), get())
            if (get<ApiConfig>().baseUrl == null) FakeAvatarGenerator(repository) else repository
        }
        factoryOf(::ObserveCharacter)
        factoryOf(::DeleteCharacter)
        factoryOf(::SaveCharacter)
        factoryOf(::GenerateCharacterAvatar)
        factoryOf(::ObserveCharacters)
        factoryOf(::RefreshCharacters)
    }
