package com.dozy.mobile.feature.character.di

import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterUiState
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsViewModel
import com.dozy.mobile.feature.character.presentation.list.CharactersViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Shared presentation registrations compose the shared character data graph. */
val characterModule =
    module {
        includes(characterDataModule)
        viewModel {
            val handle: androidx.lifecycle.SavedStateHandle = get()
            CreateCharacterViewModel(
                statePersistence = SavedStatePersistence(handle, CreateCharacterUiState.serializer()),
                saveCharacter = get(),
                generateAvatar = get(),
                observeCharacter = get(),
                initialId = handle.get<String>("id"),
            )
        }
        viewModelOf(::CharactersViewModel)
        viewModel {
            CharacterDetailsViewModel(
                get(),
                get(),
                Character.Id(requireNotNull(get<androidx.lifecycle.SavedStateHandle>().get<String>("id"))),
            )
        }
    }
