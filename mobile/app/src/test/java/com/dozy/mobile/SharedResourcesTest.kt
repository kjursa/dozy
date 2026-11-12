package com.dozy.mobile

import com.dozy.mobile.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class SharedResourcesTest {
    @org.junit.Before fun initializeSharedResources() {
        initializeComposeResourceProvider()
    }

    @Test fun androidLoadsSharedCopyWithFormattingPluralsAndEscapes() =
        runBlocking {
            assertEquals("Use at most 50 characters for the name.", getString(Res.string.character_name_too_long, 50))
            assertEquals("Anything special you'd like?", getString(Res.string.anything_special_you_d_like))
            assertEquals("How should it end?  (optional)", getString(Res.string.how_should_it_end_optional))
            assertEquals("Where shall we\ngo tonight?", getString(Res.string.story_form_heading))
            assertEquals("1 min read", getPluralString(Res.plurals.story_read_minutes, 1, 1))
            assertEquals("5 min read", getPluralString(Res.plurals.story_read_minutes, 5, 5))
        }
}
