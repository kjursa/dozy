package com.dozy.api.client.story.v1

import com.dozy.api.contracts.v1.ApiErrorResponse

sealed interface StoryReadError {
    data object NotFound : StoryReadError
    data object Unavailable : StoryReadError
    data class Unknown(val status: Int, val error: ApiErrorResponse) : StoryReadError
}
