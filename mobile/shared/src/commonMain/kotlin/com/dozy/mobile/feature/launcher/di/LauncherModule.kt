package com.dozy.mobile.feature.launcher.di

import com.dozy.mobile.feature.launcher.presentation.LauncherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val launcherModule =
    module {
        viewModel { LauncherViewModel(get(), get(), get()) }
    }
