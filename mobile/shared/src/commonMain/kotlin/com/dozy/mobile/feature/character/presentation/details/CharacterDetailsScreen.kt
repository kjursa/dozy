@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dozy.mobile.feature.character.presentation.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.resources.*
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CharacterDetailsScreen(
    viewModel: CharacterDetailsViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEdit: (Character.Id) -> Unit,
    onStory: (Character.Id) -> Unit,
) {
    androidx.lifecycle.compose.LifecycleResumeEffect(viewModel) {
        viewModel.resumed()
        onPauseOrDispose { }
    }
    com.kjursa.foundation.compose.mvi.MviScreen(viewModel, onEffect = { effect ->
        when (effect) {
            CharacterDetailsEffect.Back -> onBack()
            CharacterDetailsEffect.Deleted -> onDeleted()
            is CharacterDetailsEffect.Edit -> onEdit(effect.id)
            is CharacterDetailsEffect.Story -> onStory(effect.id)
        }
    }) { state, dispatch ->
        CharacterDetailsContent(state, dispatch)
    }
}

@Composable
internal fun CharacterDetailsContent(
    state: CharacterDetailsUiState,
    dispatch: (CharacterDetailsAction) -> Unit,
) {
    androidx.compose.ui.backhandler
        .BackHandler { dispatch(CharacterDetailsAction.Back) }
    if (state.deleteDialog) {
        AlertDialog(
            onDismissRequest = { dispatch(CharacterDetailsAction.CancelDelete) },
            title = { Text(stringResource(Res.string.character_delete_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.character_delete_description))
                    state.deleteError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                    if (state.deleting) CircularProgressIndicator()
                }
            },
            confirmButton = {
                TextButton(enabled = !state.deleting, onClick = { dispatch(CharacterDetailsAction.ConfirmDelete) }) {
                    Text(stringResource(Res.string.character_delete))
                }
            },
            dismissButton = {
                TextButton(enabled = !state.deleting, onClick = { dispatch(CharacterDetailsAction.CancelDelete) }) {
                    Text(stringResource(Res.string.character_keep))
                }
            },
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        TextButton(onClick = { dispatch(CharacterDetailsAction.Back) }) { Text(stringResource(Res.string.back)) }
        if (state.loading) {
            CircularProgressIndicator()
        } else if (state.readError != null) {
            Text(requireNotNull(state.readError).resolve())
            TextButton(onClick = { dispatch(CharacterDetailsAction.Retry) }) { Text(stringResource(Res.string.characters_retry)) }
        } else {
            val character = state.character
            if (character == null) {
                Text(stringResource(Res.string.character_unavailable))
            } else {
                AvatarImage(character.avatar?.url?.value, character.details.name.value, Modifier.size(220.dp))
                Text(character.details.name.value, style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(
                        when (character.details.type) {
                            Character.Type.CHILD -> Res.string.child
                            Character.Type.ANIMAL -> Res.string.animal
                            Character.Type.FANTASY -> Res.string.fantasy
                            Character.Type.OTHER -> Res.string.other
                        },
                    ),
                )
                Text(stringResource(Res.string.personality))
                Text(character.details.personality.value)
                Text(stringResource(Res.string.appearance))
                Text(character.details.appearance.value)
                TextButton(onClick = { dispatch(CharacterDetailsAction.Edit) }) { Text(stringResource(Res.string.character_edit)) }
                Button(onClick = { dispatch(CharacterDetailsAction.Story) }) { Text(stringResource(Res.string.character_story)) }
                TextButton(onClick = { dispatch(CharacterDetailsAction.Delete) }) { Text(stringResource(Res.string.character_delete)) }
            }
        }
    }
}
