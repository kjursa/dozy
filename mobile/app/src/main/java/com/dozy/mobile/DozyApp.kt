package com.dozy.mobile

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val MAIN_ROUTE = "main"
private const val CREATE_STORY_ROUTE = "create-story"
private const val CREATE_CHARACTER_ROUTE = "create-character"

@Composable
fun DozyApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MAIN_ROUTE) {
        composable(MAIN_ROUTE) {
            MainScreen(
                onCreateStory = { navController.navigate(CREATE_STORY_ROUTE) },
                onCreateCharacter = { navController.navigate(CREATE_CHARACTER_ROUTE) },
            )
        }
        composable(CREATE_STORY_ROUTE) {
            CreateStoryScreen(
                onBack = { navController.popBackStack() },
                onNewHero = { navController.navigate(CREATE_CHARACTER_ROUTE) },
                onGenerate = { navController.popBackStack(MAIN_ROUTE, inclusive = false) },
            )
        }
        composable(CREATE_CHARACTER_ROUTE) {
            val characterViewModel: CreateCharacterViewModel = viewModel(factory = LocalDozyViewModelFactory.current)
            CreateCharacterScreen(
                viewModel = characterViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
