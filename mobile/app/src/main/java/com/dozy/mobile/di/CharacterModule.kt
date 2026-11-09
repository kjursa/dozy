package com.dozy.mobile.di

import com.dozy.mobile.CharactersViewModel
import com.dozy.mobile.CreateCharacterViewModel
import com.dozy.mobile.HomeViewModel
import com.dozy.mobile.StoriesViewModel
import com.dozy.mobile.data.CharacterApi
import com.dozy.mobile.data.DefaultCharacterRepository
import com.dozy.mobile.data.KtorCharacterApi
import com.dozy.mobile.domain.CharacterRepository
import com.dozy.mobile.domain.ObserveCharacters
import com.dozy.mobile.domain.SaveCharacter
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
