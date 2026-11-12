package com.dozy.mobile.feature.home.presentation

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.character.domain.usecase.RefreshCharacters
import com.dozy.mobile.feature.place.domain.usecase.ObservePlaces
import com.dozy.mobile.feature.place.domain.usecase.RefreshPlaces
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.usecase.ObserveGenerations
import com.dozy.mobile.feature.story.domain.usecase.ObserveStoryLibrary
import com.dozy.mobile.feature.story.domain.usecase.RefreshGeneration
import com.dozy.mobile.feature.story.domain.usecase.RefreshStories
import com.dozy.mobile.feature.story.domain.usecase.RetryGeneration
import com.dozy.mobile.feature.story.presentation.generation.GenerationAction
import com.dozy.mobile.feature.story.presentation.generation.GenerationUiState
import com.dozy.mobile.feature.story.presentation.list.toUiStory
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.coroutines.runCatchingSuspend
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeCharacters: ObserveCharacters,
    private val observeStories: ObserveStoryLibrary,
    private val observePlaces: ObservePlaces,
    private val observeGenerations: ObserveGenerations,
    private val refreshCharacters: RefreshCharacters,
    private val refreshStories: RefreshStories,
    private val refreshPlaces: RefreshPlaces,
    private val refreshGeneration: RefreshGeneration,
    private val retryGeneration: RetryGeneration,
) : MviViewModel<HomeUiState, HomeAction, HomeEffect>(HomeUiState()) {
    private val observations = mutableMapOf<HomeSection, Job>()

    init {
        resumed()
    }

    fun resumed() {
        updateState { copy(navigating = false) }
        HomeSection.entries.forEach { load(it) }
    }

    private fun load(section: HomeSection) {
        if (section in uiState.value.refreshing || (section == HomeSection.GENERATION && uiState.value.generation.busy)) return
        if (observations[section]?.isActive != true) {
            updateState { copy(loading = loading + section) }
            observations[section] =
                when (section) {
                    HomeSection.CHARACTERS ->
                        observe(section, observeCharacters()) {
                            copy(
                                characters =
                                    it.characters
                                        .map { hero ->
                                            hero.toHomeCharacter()
                                        }.take(6),
                            )
                        }
                    HomeSection.STORIES ->
                        observe(section, observeStories()) {
                            copy(
                                stories =
                                    it.take(3).map { story ->
                                        story.toUiStory()
                                    },
                            )
                        }
                    HomeSection.PLACES -> observe(section, observePlaces()) { copy(places = it.take(3)) }
                    HomeSection.GENERATION ->
                        observe(
                            section,
                            observeGenerations(),
                        ) { copy(generation = generation.copy(loading = false, jobs = it)) }
                }
        }
        updateState { copy(refreshing = refreshing + section, errors = errors - section) }
        viewModelScope.launch {
            val failed =
                when (section) {
                    HomeSection.CHARACTERS -> refreshCharacters() == RefreshCharacters.Result.Failure
                    HomeSection.STORIES -> refreshStories() == RefreshStories.Result.Failure
                    HomeSection.PLACES -> refreshPlaces() == RefreshPlaces.Result.Failure
                    HomeSection.GENERATION ->
                        runCatchingSuspend {
                            val job = GenerationUiState(jobs = observeGenerations().first()).homeJob
                            job?.let { refreshGeneration(it.id) == RefreshGeneration.Result.Unavailable } ?: false
                        }.getOrDefault(true)
                }
            updateState { copy(refreshing = refreshing - section) }
            if (failed) sectionFailed(section)
        }
    }

    private fun <T> observe(
        section: HomeSection,
        flow: Flow<T>,
        reduce: HomeUiState.(T) -> HomeUiState,
    ): Job =
        viewModelScope.launch {
            flow.catch { sectionFailed(section) }.collect { value ->
                updateState { reduce(value).copy(loading = loading - section) }
            }
        }

    private fun sectionFailed(section: HomeSection) {
        val message =
            when (section) {
                HomeSection.CHARACTERS -> Res.string.could_not_load_characters
                HomeSection.STORIES -> Res.string.stories_load_error
                HomeSection.PLACES -> Res.string.places_load_error
                HomeSection.GENERATION -> Res.string.generation_unavailable
            }
        updateState { copy(loading = loading - section, errors = errors + (section to UiText.Resource(message))) }
    }

    override fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.RetrySection -> load(action.section)
            is HomeAction.Generation -> generationAction(action.action)
            HomeAction.CreateStoryClicked -> navigate(HomeEffect.CreateStoryRequested)
            is HomeAction.StorySelected ->
                if (uiState.value.stories.any { it.id == action.story.id }) {
                    navigate(
                        HomeEffect.OpenStory(action.story),
                    )
                }
            HomeAction.SeeAllStoriesClicked -> navigate(HomeEffect.SeeAllStoriesRequested)
            is HomeAction.CharacterSelected ->
                if (uiState.value.characters.any {
                        it.id == action.character.id
                    }
                ) {
                    navigate(HomeEffect.OpenCharacter(action.character))
                }
            HomeAction.AddCharacterClicked -> navigate(HomeEffect.AddCharacterRequested)
            HomeAction.SeeAllCharactersClicked -> navigate(HomeEffect.SeeAllCharactersRequested)
            HomeAction.SeeAllPlacesClicked -> navigate(HomeEffect.SeeAllPlacesRequested)
            is HomeAction.PlaceSelected ->
                if (uiState.value.places.any { it.id == action.id }) {
                    navigate(
                        HomeEffect.CreateStoryAtPlace(action.id),
                    )
                }
            is HomeAction.ProgressClicked -> navigate(HomeEffect.OpenProgress(action.id))
        }
    }

    private fun generationAction(action: GenerationAction) {
        if (uiState.value.generation.busy) return
        val job = uiState.value.generation.homeJob ?: return
        when (action) {
            is GenerationAction.Open -> if (job.id == action.id) job.storyId?.let { navigate(HomeEffect.ReadStory(it.value)) }
            is GenerationAction.Refresh -> load(HomeSection.GENERATION)
            is GenerationAction.Retry -> {
                if (job.id != action.id ||
                    HomeSection.GENERATION in uiState.value.refreshing ||
                    job.status != GenerationJob.Status.FAILED
                ) {
                    return
                }
                updateState { copy(generation = generation.copy(busy = true), errors = errors - HomeSection.GENERATION) }
                viewModelScope.launch {
                    val result = retryGeneration(action.id)
                    updateState { copy(generation = generation.copy(busy = false)) }
                    if (result == RetryGeneration.Result.Unavailable) sectionFailed(HomeSection.GENERATION)
                }
            }
        }
    }

    private fun navigate(effect: HomeEffect) {
        if (uiState.value.navigating) return
        updateState { copy(navigating = true) }
        sendEffect(effect)
    }
}
