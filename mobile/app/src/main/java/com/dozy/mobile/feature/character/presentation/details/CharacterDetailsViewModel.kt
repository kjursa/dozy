package com.dozy.mobile.feature.character.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.feature.character.domain.usecase.ObserveCharacters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal data class CharacterDetailsState(
    val loading: Boolean = true,
    val character: Character? = null,
)

internal class CharacterDetailsViewModel(
    observeCharacters: ObserveCharacters,
    id: String,
) : ViewModel() {
    private val state = MutableStateFlow(CharacterDetailsState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            observeCharacters().catch { state.value = CharacterDetailsState(false) }.collect { collection ->
                state.value = CharacterDetailsState(false, collection.characters.firstOrNull { it.id.value == id })
            }
        }
    }
}
