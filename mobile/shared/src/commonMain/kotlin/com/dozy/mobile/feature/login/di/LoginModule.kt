package com.dozy.mobile.feature.login.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.login.data.FakeLoginRepository
import com.dozy.mobile.feature.login.domain.repository.LoginRepository
import com.dozy.mobile.feature.login.presentation.LoginUiState
import com.dozy.mobile.feature.login.presentation.LoginViewModel
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loginModule =
    module {
        single<LoginRepository> { FakeLoginRepository() }
        viewModel {
            val savedStateHandle: SavedStateHandle = get()
            LoginViewModel(
                SavedStatePersistence(savedStateHandle, LoginUiState.serializer()),
                get(),
            )
        }
    }
