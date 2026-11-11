package com.dozy.mobile.feature.story.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.story.domain.usecase.ObserveSavedStories
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

internal class SavedStoryViewModel(
    observe: ObserveSavedStories,
) : ViewModel() {
    val stories = observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
