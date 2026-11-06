package com.dozy.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private const val HOME_ROUTE = "home"
private const val STORY_ROUTE = "create-story"
private const val CHARACTER_ROUTE = "create-character"

@Composable
fun DozyApp() {
    var route by rememberSaveable { mutableStateOf(HOME_ROUTE) }
    var characterReturnRoute by rememberSaveable { mutableStateOf(HOME_ROUTE) }

    when (route) {
        STORY_ROUTE -> CreateStoryScreen(
            onBack = { route = HOME_ROUTE },
            onNewHero = {
                characterReturnRoute = STORY_ROUTE
                route = CHARACTER_ROUTE
            },
            onGenerate = { route = HOME_ROUTE },
        )

        CHARACTER_ROUTE -> CreateCharacterScreen(
            onBack = { route = characterReturnRoute },
            onSaved = { route = characterReturnRoute },
        )

        else -> HomeScreen(
            onCreateStory = { route = STORY_ROUTE },
            onOpenCharacters = {
                characterReturnRoute = HOME_ROUTE
                route = CHARACTER_ROUTE
            },
        )
    }
}
