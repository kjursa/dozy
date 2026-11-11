package com.dozy.mobile.feature.character.di

import com.dozy.mobile.feature.character.data.DefaultCharacterRepository
import com.dozy.mobile.feature.character.data.api.CharacterApi
import com.dozy.mobile.feature.character.data.api.KtorCharacterApi
import com.dozy.mobile.feature.character.domain.repository.CharacterRepository
import com.dozy.mobile.feature.character.domain.usecase.GenerateCharacterAvatar
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.SaveCharacter
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsViewModel
import com.dozy.mobile.feature.character.presentation.list.CharactersViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Common DI graph: platform modules provide CharacterDao, HttpClient, and CharacterIdGenerator. */
internal val characterModule =
    module {
        single<CharacterApi> { KtorCharacterApi(get(), get()) }
        single<CharacterRepository> { DefaultCharacterRepository(get(), get()) }
        factoryOf(::SaveCharacter)
        factoryOf(::GenerateCharacterAvatar)
        factoryOf(::ObserveCharacters)
        viewModel {
            CreateCharacterViewModel(
                statePersistence = SavedStatePersistence(get(), CreateCharacterUiState.serializer()),
                saveCharacter = get(),
                generateAvatar = get(),
                observeCharacters = get(),
                initialId = get<androidx.lifecycle.SavedStateHandle>().get<String>("id"),
            )
        }
        viewModelOf(::CharactersViewModel)
        viewModel { CharacterDetailsViewModel(get(), requireNotNull(get<androidx.lifecycle.SavedStateHandle>().get<String>("id"))) }
    }
