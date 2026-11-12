package com.dozy.mobile.di

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.character.di.characterModule
import com.dozy.mobile.feature.home.di.homeModule
import com.dozy.mobile.feature.place.di.placeModule
import com.dozy.mobile.feature.story.data.notification.UnsupportedStoryCompletionNotifier
import com.dozy.mobile.feature.story.di.storyModule
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier
import org.koin.dsl.module

/**
 * Presentation for a future iOS host. SavedStateHandle is an in-memory store here;
 * there is no process-death or cross-relaunch restoration on iOS.
 * The host supplies a handle with route arguments via parametersOf(handle) when
 * creating a form/detail/reader ViewModel and retains it with that navigation owner.
 * The default factory supports argument-free new forms only.
 */
val iosPresentationModule =
    module {
        includes(characterModule, homeModule, placeModule, storyModule)
        factory { SavedStateHandle() }
        single<StoryCompletionNotifier> { UnsupportedStoryCompletionNotifier() }
    }
