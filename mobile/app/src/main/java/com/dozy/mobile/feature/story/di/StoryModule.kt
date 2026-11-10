package com.dozy.mobile.feature.story.di

import com.dozy.mobile.feature.story.presentation.list.StoriesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val storyModule = module { viewModelOf(::StoriesViewModel) }
