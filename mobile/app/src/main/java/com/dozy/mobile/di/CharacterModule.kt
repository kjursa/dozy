package com.dozy.mobile.di

import com.dozy.mobile.data.character.DefaultCharacterRepository
import com.dozy.mobile.data.character.api.CharacterApi
import com.dozy.mobile.data.character.api.KtorCharacterApi
import com.dozy.mobile.domain.character.CharacterRepository
import com.dozy.mobile.domain.character.ObserveCharacters
import com.dozy.mobile.domain.character.SaveCharacter
import com.dozy.mobile.presentation.characters.CharactersViewModel
import com.dozy.mobile.presentation.createcharacter.CreateCharacterViewModel
import com.dozy.mobile.presentation.home.HomeViewModel
import com.dozy.mobile.presentation.stories.StoriesViewModel
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
        viewModelOf(::HomeViewModel)
        viewModelOf(::CharactersViewModel)
        viewModelOf(::StoriesViewModel)
    }
