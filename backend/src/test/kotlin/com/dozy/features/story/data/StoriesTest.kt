package com.dozy.features.story.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoriesTest {
    @Test
    fun `offers five distinct bedtime stories of about one hundred words`() {
        val stories = with(Stories) { setOf(story1, story2, story3, story4, story5) }
        assertEquals(5, stories.size)
        assertTrue(Stories.random() in stories)
        for (story in stories) {
            val body = story.substringAfter('\n')
            assertTrue(body.split(Regex("\\s+")).size in 90..110)
            assertTrue("{heroes}" in body && "{place}" in body)
        }
    }
}
