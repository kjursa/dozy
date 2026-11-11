package com.dozy.mobile.feature.story.presentation.create

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class CreateStoryViewModel(
    persistence: StatePersistence<CreateStoryUiState>,
    initialPlaceId: String?,
    private val selectPlace: SelectPlace,
    private val observeCharacters: ObserveCharacters,
) : MviViewModel<CreateStoryUiState, CreateStoryAction, CreateStoryEffect>(
        CreateStoryUiState(placeId = initialPlaceId, randomSetting = initialPlaceId == null),
        persistence,
    ) {
    init {
        resolveName()
        viewModelScope.launch {
            observeCharacters().catch { updateState { copy(characterLoadError = true) } }.collect {
                updateState {
                    copy(
                        heroes = it.characters,
                        selectedHeroes = selectedHeroes.filter { id -> it.characters.any { hero -> hero.id.value == id } },
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

    private var openingPlaces = false

    fun resumed() {
        openingPlaces = false
    }

    override fun onAction(action: CreateStoryAction) {
        when (action) {
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
