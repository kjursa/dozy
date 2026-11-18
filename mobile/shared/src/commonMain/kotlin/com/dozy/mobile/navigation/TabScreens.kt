package com.dozy.mobile.navigation

import androidx.compose.runtime.Composable
import com.dozy.mobile.feature.character.presentation.list.CharactersTabScreen
import com.dozy.mobile.feature.home.presentation.HomeScreen
import com.dozy.mobile.feature.story.presentation.list.StoriesTabScreen
import com.kjursa.foundation.compose.navigation.Screen

class HomeTabScreen : Screen {
    @Composable override fun Content() {
        HomeScreen()
    }
}

class StoriesTabEntry : Screen {
    @Composable override fun Content() {
        StoriesTabScreen()
    }
}

class CharactersTabEntry : Screen {
    @Composable override fun Content() {
        CharactersTabScreen()
    }
}
