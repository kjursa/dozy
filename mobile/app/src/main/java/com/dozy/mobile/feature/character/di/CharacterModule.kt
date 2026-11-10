package com.dozy.mobile.feature.character.di

import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.api.CharacterApi
import com.dozy.mobile.feature.character.data.api.KtorCharacterApi
import com.dozy.mobile.feature.character.domain.CharacterRepository
import com.dozy.mobile.feature.character.domain.ObserveCharacters
import com.dozy.mobile.feature.character.domain.SaveCharacter
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.list.CharactersViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Common DI graph: platform modules provide CharacterDao, HttpClient, and CharacterIdGenerator. */
internal val characterModule =
    module {
        // The backend has no character endpoint yet. Enable only after agreeing its contract.
        single<CharacterApi> { KtorCharacterApi(get(), baseUrl = null) }
        single<CharacterRepository> { DefaultCharacterRepository(get(), get()) }
        factoryOf(::SaveCharacter)
        factoryOf(::ObserveCharacters)
        viewModelOf(::CreateCharacterViewModel)
        viewModelOf(::CharactersViewModel)
    }
