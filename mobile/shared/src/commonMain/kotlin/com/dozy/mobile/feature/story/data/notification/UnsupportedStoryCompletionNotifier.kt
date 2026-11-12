package com.dozy.mobile.feature.story.data.notification

import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier

/** For hosts without system notification support. Never records delivery as successful. */
class UnsupportedStoryCompletionNotifier : StoryCompletionNotifier {
    override suspend fun deliver(id: Story.Id): Boolean = false
}
