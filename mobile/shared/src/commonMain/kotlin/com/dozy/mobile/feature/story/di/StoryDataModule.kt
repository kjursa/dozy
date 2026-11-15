package com.dozy.mobile.feature.story.di

import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.KtorLegacyStoryApi
import com.dozy.mobile.feature.story.data.api.LegacyStoryApi
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.dozy.mobile.feature.story.domain.usecase.StartGeneration
import com.dozy.mobile.feature.story.domain.usecase.ObserveGenerations
import com.dozy.mobile.feature.story.domain.usecase.ObserveSavedStories
import com.dozy.mobile.feature.story.domain.usecase.ObserveStoryLibrary
import com.dozy.mobile.feature.story.domain.usecase.RefreshStories
import com.dozy.mobile.feature.story.domain.usecase.ReadStory
import com.dozy.mobile.feature.story.domain.usecase.RefreshGeneration
import com.dozy.mobile.feature.story.domain.usecase.RetryGeneration
import com.dozy.mobile.feature.story.domain.usecase.ReconcileGenerations
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val storyDataModule =
    module {
        single<LegacyStoryApi> { KtorLegacyStoryApi(get(), get()) }
        single<GenerationRepository> { DefaultGenerationRepository(get(), get(), get(), get()) }
        factoryOf(::StartGeneration)
        factoryOf(::ObserveGenerations)
        factoryOf(::ObserveSavedStories)
        factoryOf(::ObserveStoryLibrary)
        factoryOf(::RefreshStories)
        factoryOf(::ReadStory)
        factoryOf(::RefreshGeneration)
        factoryOf(::RetryGeneration)
        factoryOf(::ReconcileGenerations)
    }
