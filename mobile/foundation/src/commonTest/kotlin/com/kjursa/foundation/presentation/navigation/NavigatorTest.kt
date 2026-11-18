package com.kjursa.foundation.presentation.navigation

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigatorTest {
    @Test
    fun queuesCommandsUntilHostConnectsInOrder() =
        runTest {
            val navigator = Navigator<String>()
            navigator.navigate("first")
            navigator.navigate("second")
            assertEquals("first", navigator.commands.first())
            assertEquals("second", navigator.commands.first())
        }

    @Test
    fun consumedCommandsAreNotReplayedToRecreatedHost() =
        runTest {
            val navigator = Navigator<String>()
            navigator.navigate("open")
            assertEquals("open", navigator.commands.first())
            assertNull(withTimeoutOrNull(1) { navigator.commands.first() })
            navigator.navigate("back")
            assertEquals("back", navigator.commands.first())
        }
}
