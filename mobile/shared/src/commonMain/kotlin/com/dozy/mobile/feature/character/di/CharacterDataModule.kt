package com.dozy.mobile.feature.character.di

import com.dozy.api.client.DozyClient
import com.dozy.api.client.character.CharactersClients
import com.dozy.api.client.characteravatar.CharacterAvatarsClients
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.FakeAvatarGenerator
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
        single<CharactersClients> { get<DozyClient>().characters }
        single<CharacterAvatarsClients> { get<DozyClient>().characterAvatars }
        single<CharacterRepository> {
            val repository = DefaultCharacterRepository(get(), get(), get(), get())
            if (get<ApiConfig>().baseUrl == null) FakeAvatarGenerator(repository) else repository
        }
        factoryOf(::ObserveCharacter)
        factoryOf(::DeleteCharacter)
        factoryOf(::SaveCharacter)
        factoryOf(::GenerateCharacterAvatar)
        factoryOf(::ObserveCharacters)
        factoryOf(::RefreshCharacters)
    }
