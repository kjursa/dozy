package com.dozy.mobile

import com.dozy.api.contracts.v1.character.AvatarDto
import com.dozy.mobile.feature.story.data.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StoryAvatarUrlTest {
    @Test
    fun localAvatarInCompletedStoryRequiresDevelopmentHttp() {
        val story = ProgressApi().completedStory()
        val url = "http://10.0.2.2:8080/assets/avatar"
        val cast = story.configuration.cast.single().copy(avatar = AvatarDto("avatar", url))
        val response = story.copy(configuration = story.configuration.copy(cast = listOf(cast)))

        assertFailsWith<IllegalArgumentException> { response.toDomain() }
        assertEquals(url, response.toDomain(allowLocalAssetHttp = true).configuration.cast.single().avatar?.url?.value)
    }
}
