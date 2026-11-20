package com.dozy.mobile.feature.login.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.launcher.di.launcherModule
import com.dozy.mobile.feature.login.data.ApiLoginRepository
import com.dozy.mobile.feature.login.domain.repository.LoginRepository
import com.dozy.mobile.feature.login.presentation.LoginUiState
import com.dozy.mobile.feature.login.presentation.LoginViewModel
import com.dozy.mobile.navigation.navigationModule
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loginModule =
    module {
        includes(navigationModule, launcherModule)
        single<LoginRepository> { ApiLoginRepository(get(), get()) }
        viewModel {
            val savedStateHandle: SavedStateHandle = get()
            LoginViewModel(
                get(),
                SavedStatePersistence(savedStateHandle, LoginUiState.serializer()),
                get(),
            )
        }
    }
