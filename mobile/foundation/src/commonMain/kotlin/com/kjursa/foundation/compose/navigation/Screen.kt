package com.kjursa.foundation.compose.navigation

import androidx.compose.runtime.Composable

/** A route entry point. Resolve lifecycle-bound state inside Content, never in the constructor. */
interface Screen {
    @Composable
    fun Content()
}
