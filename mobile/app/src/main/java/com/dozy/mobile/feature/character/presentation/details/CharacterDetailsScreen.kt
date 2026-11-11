package com.dozy.mobile.feature.character.presentation.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dozy.mobile.R
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.feature.character.domain.model.Character

@Composable
internal fun CharacterDetailsScreen(
    viewModel: CharacterDetailsViewModel,
    onBack: () -> Unit,
    onEdit: (Character.Id) -> Unit,
    onStory: (Character.Id) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        if (state.loading) {
            CircularProgressIndicator()
        } else {
            val character = state.character
            if (character == null) {
                Text(stringResource(R.string.character_unavailable))
            } else {
                AvatarImage(character.avatar?.url?.value, character.details.name.value, Modifier.size(220.dp))
                Text(character.details.name.value, style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(
                        when (character.details.type) {
                            Character.Type.CHILD -> R.string.child
                            Character.Type.ANIMAL -> R.string.animal
                            Character.Type.FANTASY -> R.string.fantasy
                            Character.Type.OTHER -> R.string.other
                        },
                    ),
                )
                Text(character.details.personality.value)
                Text(character.details.appearance.value)
                TextButton(onClick = { onEdit(character.id) }) { Text(stringResource(R.string.character_edit)) }
                Button(onClick = { onStory(character.id) }) { Text(stringResource(R.string.character_story)) }
            }
        }
    }
}
