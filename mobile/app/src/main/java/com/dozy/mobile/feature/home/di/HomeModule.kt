package com.dozy.mobile.feature.home.di

import com.dozy.mobile.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val homeModule = module { viewModelOf(::HomeViewModel) }
