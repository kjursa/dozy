package com.dozy.mobile

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterScreen
import com.dozy.mobile.feature.character.presentation.create.CreateCharacterViewModel
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsScreen
import com.dozy.mobile.feature.character.presentation.details.CharacterDetailsViewModel
import com.dozy.mobile.feature.place.presentation.form.PlaceFormEffect
import com.dozy.mobile.feature.place.presentation.form.PlaceFormScreen
import com.dozy.mobile.feature.place.presentation.form.PlaceFormViewModel
import com.dozy.mobile.feature.place.presentation.list.PlacesAction
import com.dozy.mobile.feature.place.presentation.list.PlacesEffect
import com.dozy.mobile.feature.place.presentation.list.PlacesScreen
import com.dozy.mobile.feature.place.presentation.list.PlacesViewModel
import com.dozy.mobile.feature.story.presentation.create.CreateStoryAction
import com.dozy.mobile.feature.story.presentation.create.CreateStoryScreen
import com.dozy.mobile.feature.story.presentation.create.CreateStoryViewModel
import com.dozy.mobile.navigation.CREATE_CHARACTER_ROUTE
import com.dozy.mobile.navigation.CREATE_STORY_ROUTE
import com.dozy.mobile.navigation.GENERATION_ROUTE
import com.dozy.mobile.navigation.MAIN_ROUTE
import com.dozy.mobile.navigation.MainScreen
import com.dozy.mobile.navigation.PLACES_ROUTE
import com.dozy.mobile.navigation.PLACE_FORM_ROUTE
import com.dozy.mobile.navigation.characterDeleted
import com.dozy.mobile.navigation.characterSaved
import com.dozy.mobile.navigation.generationAccepted
import com.dozy.mobile.navigation.openStory
import com.dozy.mobile.navigation.usePlace
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DozyApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MAIN_ROUTE) {
        composable(MAIN_ROUTE) { entry ->
            val showCharacters by entry.savedStateHandle.getStateFlow("showCharacters", false).collectAsStateWithLifecycle()
            MainScreen(
                showCharacters = showCharacters,
                onCharactersShown = { entry.savedStateHandle["showCharacters"] = false },
                onCreateStory = { navController.openStory(null) },
                onCreateCharacter = { navController.navigate("create-character") },
                onCharacter = { navController.navigate("character/${Uri.encode(it)}") },
                onPlaces = { navController.navigate("places?fromStory=false") { launchSingleTop = true } },
                onPlaceSelected = { navController.openStory(it) },
            )
        }
        composable(CREATE_STORY_ROUTE) { entry ->
            val storyViewModel: CreateStoryViewModel = koinViewModel()
            androidx.compose.runtime.LaunchedEffect(entry) {
                entry.savedStateHandle.getStateFlow<String?>("newHero", null).collect { id ->
                    if (id != null) {
                        storyViewModel.dispatch(CreateStoryAction.AddHero(id))
                        entry.savedStateHandle["newHero"] = null
                    }
                }
            }
            CreateStoryScreen(
                viewModel = storyViewModel,
                onBack = { navController.popBackStack() },
                onNewHero = { navController.navigate("create-character") },
                onChoosePlace = { navController.navigate("places?fromStory=true") { launchSingleTop = true } },
                onGenerate = { jobId ->
                    navController.generationAccepted(jobId)
                },
            )
        }
        composable(GENERATION_ROUTE) { entry ->
            com.dozy.mobile.feature.story.presentation.create.AcceptedGenerationScreen(
                requireNotNull(entry.arguments?.getString("jobId")),
                onHome = { navController.popBackStack(MAIN_ROUTE, inclusive = false) },
            )
        }
        composable(PLACES_ROUTE) { entry ->
            val viewModel: PlacesViewModel = koinViewModel()
            val fromStory = entry.arguments?.getString("fromStory") == "true"
            val draft: CreateStoryViewModel? =
                if (fromStory) {
                    koinViewModel(
                        viewModelStoreOwner = navController.getBackStackEntry(CREATE_STORY_ROUTE),
                    )
                } else {
                    null
                }
            PlacesScreen(viewModel) { effect ->
                when (effect) {
                    PlacesEffect.Back -> navController.popBackStack()
                    is PlacesEffect.Form ->
                        navController.navigate(
                            if (effect.id ==
                                null
                            ) {
                                "place-form"
                            } else {
                                "place-form?id=${Uri.encode(effect.id.value)}"
                            },
                        ) { launchSingleTop = true }
                    is PlacesEffect.Selected -> navController.usePlace(effect.id, draft)
                }
            }
        }
        composable(PLACE_FORM_ROUTE) {
            val viewModel: PlaceFormViewModel = koinViewModel()
            val list: PlacesViewModel = koinViewModel(viewModelStoreOwner = navController.getBackStackEntry(PLACES_ROUTE))
            PlaceFormScreen(viewModel) { effect ->
                when (effect) {
                    PlaceFormEffect.Back -> navController.popBackStack()
                    is PlaceFormEffect.Saved -> {
                        list.dispatch(PlacesAction.Select(effect.id))
                        navController.popBackStack()
                    }
                }
            }
        }
        composable("character/{id}") {
            val vm: CharacterDetailsViewModel = koinViewModel()
            CharacterDetailsScreen(
                vm,
                onDeleted = { navController.characterDeleted() },
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("create-character?id=${Uri.encode(it.value)}") },
                onStory = {
                    navController.openStory(null, listOf(it))
                },
            )
        }
        composable(CREATE_CHARACTER_ROUTE) {
            val characterViewModel: CreateCharacterViewModel = koinViewModel()
            val draft: CreateStoryViewModel? =
                if (navController.previousBackStackEntry?.destination?.route == CREATE_STORY_ROUTE) {
                    koinViewModel(viewModelStoreOwner = navController.getBackStackEntry(CREATE_STORY_ROUTE))
                } else {
                    null
                }
            CreateCharacterScreen(
                viewModel = characterViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { id -> navController.characterSaved(id, draft) },
            )
        }
    }
}
