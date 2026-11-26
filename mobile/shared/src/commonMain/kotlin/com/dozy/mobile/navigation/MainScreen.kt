package com.dozy.mobile.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dozy.mobile.core.presentation.components.LocalMainTabHazeState
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.characters
import com.dozy.mobile.resources.home
import com.dozy.mobile.resources.my_stories
import com.dozy.mobile.ui.theme.BottomTabPalette
import com.kjursa.foundation.compose.theme.UiSpacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val TAB_EDGE_ANIMATION_MS = 160

private const val HOME_TAB = "home"
private const val STORIES_TAB = "stories"
private const val CHARACTERS_TAB = "characters"

private fun bottomTabPalette(route: String): BottomTabPalette =
    when (route) {
        STORIES_TAB -> BottomTabPalette.STORIES
        CHARACTERS_TAB -> BottomTabPalette.CHARACTERS
        else -> BottomTabPalette.HOME
    }

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
    val selectTab: (String) -> Unit = { route ->
        if (route != selectedRoute) {
            tabController.navigate(route) {
                popUpTo(tabController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalMainTabHazeState provides hazeState) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavHost(
                navController = tabController,
                startDestination = HOME_TAB,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable(HOME_TAB) { homeScreen.Content() }
                composable(STORIES_TAB) { storiesScreen.Content() }
                composable(CHARACTERS_TAB) { charactersScreen.Content() }
            }

            MainBottomPanel(
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f),
                hazeState = hazeState,
                selectedRoute = selectedRoute,
                onSelect = selectTab,
            )
        }
    }
}

@Composable
private fun MainBottomPanel(
    modifier: Modifier,
    hazeState: HazeState,
    selectedRoute: String,
    onSelect: (String) -> Unit,
) {
    val shape = RoundedCornerShape(40.dp)
    val surface = MaterialTheme.colorScheme.surface
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = UiSpacing.x3)
                .navigationBarsPadding()
                .padding(bottom = 4.dp)
                .shadow(20.dp, shape)
                .clip(shape)
                .hazeEffect(
                    state = hazeState,
                    style =
                        HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.background,
                            tint = HazeTint(surface.copy(alpha = 0.66f)),
                            fallbackTint = HazeTint(surface),
                            blurRadius = 16.dp,
                            noiseFactor = 0f,
                        ),
                ),
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        val selectedIndex =
            when (selectedRoute) {
                STORIES_TAB -> 1
                CHARACTERS_TAB -> 2
                else -> 0
            }
        Row(
            Modifier.padding(UiSpacing.x1).bottomTabIndicator(selectedIndex, bottomTabPalette(selectedRoute).background),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
private fun Modifier.bottomTabIndicator(
    selectedIndex: Int,
    background: Color,
): Modifier {
    val start = remember { Animatable(selectedIndex.toFloat()) }
    val end = remember { Animatable(selectedIndex + 1f) }
    val color by animateColorAsState(
        targetValue = background,
        animationSpec = tween(durationMillis = TAB_EDGE_ANIMATION_MS * 2),
        label = "Bottom tab background",
    )

    LaunchedEffect(selectedIndex) {
        val targetStart = selectedIndex.toFloat()
        val targetEnd = targetStart + 1f
        val edgeAnimation = tween<Float>(durationMillis = TAB_EDGE_ANIMATION_MS)
        if (targetStart >= start.value) {
            end.animateTo(targetEnd, edgeAnimation)
            start.animateTo(targetStart, edgeAnimation)
        } else {
            start.animateTo(targetStart, edgeAnimation)
            end.animateTo(targetEnd, edgeAnimation)
        }
    }

    return drawBehind {
        val tabWidth = size.width / 3f
        val width = (end.value - start.value) * tabWidth
        val left =
            if (layoutDirection == LayoutDirection.Ltr) {
                start.value * tabWidth
            } else {
                size.width - end.value * tabWidth
            }
        drawRoundRect(
            color = color,
            topLeft = Offset(left, 0f),
            size = Size(width, size.height),
            cornerRadius = CornerRadius(32.dp.toPx()),
        )
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
    val tint by animateColorAsState(
        targetValue = if (selected) bottomTabPalette(route).active else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = TAB_EDGE_ANIMATION_MS * 2),
        label = "Bottom tab tint",
    )
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(32.dp))
                .clickable(interactionSource = null, indication = null) { onSelect(route) }
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
