package com.dozy.mobile.feature.story.di

import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.api.KtorGenerationApi
import com.dozy.mobile.feature.story.domain.repository.GenerationRepository
import com.dozy.mobile.feature.story.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val storyDataModule =
    module {
        single<GenerationApi> { KtorGenerationApi(get(), get()) }
        single<GenerationRepository> { DefaultGenerationRepository(get(), get()) }
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
