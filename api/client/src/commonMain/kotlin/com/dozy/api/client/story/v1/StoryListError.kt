package com.dozy.api.client.story.v1

import com.dozy.api.contracts.v1.ApiErrorResponse
import com.dozy.api.contracts.v1.ValidationReason
import com.dozy.api.contracts.v1.story.StoryListField

sealed interface StoryListError {
    data object InvalidCursor : StoryListError
    data object Unavailable : StoryListError
    data class Validation(val field: StoryListField, val reason: ValidationReason) : StoryListError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : StoryListError
}
