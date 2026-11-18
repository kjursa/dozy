package com.dozy.mobile.feature.home.di

import com.dozy.mobile.feature.home.presentation.HomeViewModel
import com.dozy.mobile.navigation.navigationModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule =
    module {
        includes(navigationModule)
        viewModelOf(::HomeViewModel)
    }
