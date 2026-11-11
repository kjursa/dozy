package com.dozy.mobile.feature.story.presentation.create

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.R
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.story.domain.model.GenerationInput
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.domain.usecase.GenerationKeyGenerator
import com.dozy.mobile.feature.story.domain.usecase.StartGeneration
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import com.kjursa.foundation.presentation.text.UiText
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class CreateStoryViewModel(
    persistence: StatePersistence<CreateStoryUiState>,
    initialPlaceId: String?,
    private val selectPlace: SelectPlace,
    private val observeCharacters: ObserveCharacters,
    private val startGeneration: StartGeneration,
    private val keyGenerator: GenerationKeyGenerator,
    initialCast: List<String> = emptyList(),
) : MviViewModel<CreateStoryUiState, CreateStoryAction, CreateStoryEffect>(
        CreateStoryUiState(placeId = initialPlaceId, randomSetting = initialPlaceId == null, selectedHeroes = initialCast),
        persistence,
    ) {
    private var characterObservation: kotlinx.coroutines.Job? = null

    init {
        resolveName()
        loadCharacters()
    }

    private fun loadCharacters() {
        characterObservation?.cancel()
        updateState { copy(characterLoading = true, characterLoadError = false) }
        characterObservation =
            viewModelScope.launch {
                observeCharacters().catch { updateState { copy(characterLoading = false, characterLoadError = true) } }.collect { collection ->
                    updateState {
                        val selected = selectedHeroes.filter { id -> collection.characters.any { hero -> hero.id.value == id } }
                        copy(
                            heroes = collection.characters,
                            selectedHeroes = selected,
                            requestKey = if (selected != selectedHeroes) null else requestKey,
                            characterLoading = false,
                            characterLoadError = false,
                        )
                    }
                }
            }
    }

    private fun resolveName() {
        val id = uiState.value.placeId ?: return
        viewModelScope.launch {
            val result = selectPlace(Place.Id(id))
            if (uiState.value.placeId == id && result is SelectPlace.Result.Selected) {
                updateState { copy(placeName = result.place.name.value) }
            }
        }
    }

    private fun submit() {
        val state = uiState.value
        when (val validation = state.validateInput()) {
            is GenerationInput.Validation.Invalid -> updateState { withErrors(validation.errors) }
            is GenerationInput.Validation.Valid -> {
                val key = state.requestKey ?: keyGenerator.next().value
                updateState { copy(submitting = true, requestKey = key, submissionError = null) }
                viewModelScope.launch {
                    val result = startGeneration(validation.input, GenerationJob.RequestKey(key))
                    when (result) {
                        is StartGeneration.Result.Accepted -> {
                            updateState { copy(submitting = false, acceptedJobId = result.job.id.value) }
                            sendEffect(CreateStoryEffect.Accepted(result.job.id.value))
                        }
                        else ->
                            updateState {
                                copy(
                                    submitting = false,
                                    submissionError =
                                        UiText.Resource(
                                            when (result) {
                                                StartGeneration.Result.Unavailable -> R.string.story_backend_unavailable
                                                StartGeneration.Result.SelectionUnavailable -> R.string.story_selection_unavailable
                                                else -> R.string.story_acceptance_failed
                                            },
                                        ),
                                )
                            }
                    }
                }
            }
        }
    }

    private var openingPlaces = false

    fun resumed() {
        openingPlaces = false
        uiState.value.acceptedJobId?.let { sendEffect(CreateStoryEffect.Accepted(it)) }
    }

    override fun onAction(action: CreateStoryAction) {
        if (uiState.value.submitting || uiState.value.acceptedJobId != null) return
        val previous = uiState.value
        val changesRequest =
            when (action) {
                is CreateStoryAction.Automatic -> action.enabled != previous.automaticPlot
                is CreateStoryAction.AddHero -> action.id !in previous.selectedHeroes
                is CreateStoryAction.Hero -> true
                is CreateStoryAction.Length -> action.index != previous.selectedLength
                is CreateStoryAction.Setting -> !previous.automaticPlot && action.id != previous.placeId
                is CreateStoryAction.Request -> !previous.automaticPlot && action.value.trim() != previous.specialRequest.trim()
                is CreateStoryAction.Ending -> !previous.automaticPlot && action.value.trim() != previous.ending.trim()
                else -> false
            }
        if (action != CreateStoryAction.Submit && action != CreateStoryAction.ChoosePlace) {
            updateState {
                copy(
                    requestKey = if (changesRequest) null else requestKey,
                    castError = null,
                    lengthError = null,
                    ideaError = null,
                    endingError = null,
                    submissionError = null,
                )
            }
        }
        when (action) {
            is CreateStoryAction.Automatic -> updateState { copy(automaticPlot = action.enabled) }
            CreateStoryAction.Submit -> submit()
            CreateStoryAction.ReloadCharacters -> loadCharacters()
            is CreateStoryAction.Setting -> {
                updateState { copy(placeId = action.id, randomSetting = action.id == null, placeName = null) }
                resolveName()
            }
            is CreateStoryAction.AddHero -> updateState { copy(selectedHeroes = (selectedHeroes + action.id).distinct()) }
            is CreateStoryAction.Hero ->
                updateState {
                    copy(
                        selectedHeroes =
                            if (action.id in
                                selectedHeroes
                            ) {
                                selectedHeroes - action.id
                            } else {
                                selectedHeroes + action.id
                            },
                    )
                }
            is CreateStoryAction.Length -> updateState { copy(selectedLength = action.index) }
            is CreateStoryAction.Request -> updateState { copy(specialRequest = action.value) }
            is CreateStoryAction.Ending -> updateState { copy(ending = action.value) }
            CreateStoryAction.ChoosePlace ->
                if (!openingPlaces) {
                    openingPlaces = true
                    sendEffect(CreateStoryEffect.OpenPlaces)
                }
        }
    }
}
