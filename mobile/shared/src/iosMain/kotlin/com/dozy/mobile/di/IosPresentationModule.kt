package com.dozy.mobile.di

import com.dozy.mobile.feature.character.di.characterModule
import com.dozy.mobile.feature.home.di.homeModule
import com.dozy.mobile.feature.place.di.placeModule
import com.dozy.mobile.feature.story.data.notification.UnsupportedStoryCompletionNotifier
import com.dozy.mobile.feature.story.di.storyModule
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier
import org.koin.dsl.module

/** Navigation supplies each ViewModel's SavedStateHandle and route arguments. */
val iosPresentationModule =
    module {
        includes(characterModule, homeModule, placeModule, storyModule, imageModule)
        single<coil3.PlatformContext> { coil3.PlatformContext.INSTANCE }
        single<StoryCompletionNotifier> { UnsupportedStoryCompletionNotifier() }
    }
