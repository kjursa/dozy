package com.dozy.mobile.feature.story.data.api

import com.dozy.api.contracts.v1.story.GenerationRequestDto
import com.dozy.api.contracts.v1.story.toRequestBody as sharedRequestBody

internal fun GenerationRequestDto.toRequestBody() = sharedRequestBody()
