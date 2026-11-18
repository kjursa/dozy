package com.dozy.mobile.feature.story.presentation.generation

import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.navigation.NavigationCommand

sealed interface GenerationDestination : NavigationCommand {
    data class Read(
        val id: Story.Id,
    ) : GenerationDestination
}
