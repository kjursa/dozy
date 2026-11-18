package com.dozy.mobile.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.characters
import com.dozy.mobile.resources.home
import com.dozy.mobile.resources.my_stories
import com.kjursa.foundation.compose.theme.UiSpacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val HOME_TAB = "home"
private const val STORIES_TAB = "stories"
private const val CHARACTERS_TAB = "characters"

@Composable
fun MainScreen(
    showStories: Boolean = false,
    onStoriesShown: () -> Unit = {},
    showCharacters: Boolean = false,
    onCharactersShown: () -> Unit = {},
) {
    val homeScreen: HomeTabScreen = koinInject()
    val storiesScreen: StoriesTabEntry = koinInject()
    val charactersScreen: CharactersTabEntry = koinInject()
    val tabController = rememberNavController()
    LaunchedEffect(showCharacters) {
        if (showCharacters) {
            tabController.navigate(CHARACTERS_TAB) {
                popUpTo(tabController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onCharactersShown()
        }
    }
    LaunchedEffect(showStories) {
        if (showStories) {
            tabController.navigate(STORIES_TAB) {
                popUpTo(tabController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onStoriesShown()
        }
    }
    val backStackEntry by tabController.currentBackStackEntryAsState()
    val selectedRoute = backStackEntry?.destination?.route ?: HOME_TAB

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = tabController,
            startDestination = HOME_TAB,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(HOME_TAB) { homeScreen.Content() }
            composable(STORIES_TAB) { storiesScreen.Content() }
            composable(CHARACTERS_TAB) { charactersScreen.Content() }
        }

        MainBottomPanel(
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f),
            selectedRoute = selectedRoute,
            onSelect = { route ->
                if (route != selectedRoute) {
                    tabController.navigate(route) {
                        popUpTo(tabController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
        )
    }
}

@Composable
private fun MainBottomPanel(
    modifier: Modifier,
    selectedRoute: String,
    onSelect: (String) -> Unit,
) {
    val shape = RoundedCornerShape(40.dp)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = UiSpacing.x3)
                .navigationBarsPadding()
                .padding(bottom = UiSpacing.x1)
                .shadow(20.dp, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Row(Modifier.padding(UiSpacing.x1), verticalAlignment = Alignment.CenterVertically) {
            MainBottomItem(stringResource(Res.string.home), Icons.Default.Home, HOME_TAB, selectedRoute, onSelect, Modifier.weight(1f))
            MainBottomItem(
                stringResource(Res.string.my_stories),
                Icons.Default.Book,
                STORIES_TAB,
                selectedRoute,
                onSelect,
                Modifier.weight(1f),
            )
            MainBottomItem(
                stringResource(Res.string.characters),
                Icons.Default.Groups,
                CHARACTERS_TAB,
                selectedRoute,
                onSelect,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MainBottomItem(
    label: String,
    icon: ImageVector,
    route: String,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = route == selectedRoute
    val tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(32.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    RoundedCornerShape(32.dp),
                ).clickable { onSelect(route) }
                .padding(vertical = UiSpacing.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
