@file:OptIn(ExperimentalComposeUiApi::class)

package com.dozy.mobile.feature.character.presentation.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.animal
import com.dozy.mobile.resources.appearance
import com.dozy.mobile.resources.back
import com.dozy.mobile.resources.character_delete
import com.dozy.mobile.resources.character_delete_description
import com.dozy.mobile.resources.character_delete_title
import com.dozy.mobile.resources.character_details_animal_tag
import com.dozy.mobile.resources.character_details_child_tag
import com.dozy.mobile.resources.character_details_fantasy_tag
import com.dozy.mobile.resources.character_details_intro
import com.dozy.mobile.resources.character_details_other_tag
import com.dozy.mobile.resources.character_details_title
import com.dozy.mobile.resources.character_edit
import com.dozy.mobile.resources.character_keep
import com.dozy.mobile.resources.character_story_named
import com.dozy.mobile.resources.character_unavailable
import com.dozy.mobile.resources.characters_retry
import com.dozy.mobile.resources.child
import com.dozy.mobile.resources.fantasy
import com.dozy.mobile.resources.other
import com.dozy.mobile.resources.personality
import com.dozy.mobile.ui.common.DozySecondaryButton
import com.dozy.mobile.ui.theme.DozyColors
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CharacterDetailsScreen(viewModel: CharacterDetailsViewModel) {
    LifecycleResumeEffect(viewModel) {
        viewModel.resumed()
        onPauseOrDispose { }
    }
    MviScreen(viewModel) { state, dispatch -> CharacterDetailsContent(state, dispatch) }
}

@Composable
internal fun CharacterDetailsContent(
    state: CharacterDetailsUiState,
    dispatch: (CharacterDetailsAction) -> Unit,
) {
    BackHandler { dispatch(CharacterDetailsAction.Back) }
    if (state.deleteDialog) DeleteCharacterDialog(state, dispatch)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0x134ECDC4), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.8f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0BFF6B6B), Color.Transparent),
                        center = Offset(0f, size.height * 0.18f),
                        radius = size.width * 0.65f,
                    ),
                )
            }.statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        CharacterDetailsTopBar(onBack = { dispatch(CharacterDetailsAction.Back) }, onEdit = { dispatch(CharacterDetailsAction.Edit) })
        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.readError != null) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.readError.resolve())
                TextButton(onClick = { dispatch(CharacterDetailsAction.Retry) }) { Text(stringResource(Res.string.characters_retry)) }
            }
        } else {
            val character = state.character
            if (character == null) {
                Text(stringResource(Res.string.character_unavailable), Modifier.padding(24.dp))
            } else {
                CharacterDetailsBody(character, dispatch)
            }
        }
    }
}

@Composable
private fun CharacterDetailsTopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, top = 12.dp, end = 22.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CharacterDetailsIconButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = stringResource(Res.string.back),
        )
        Text(
            stringResource(Res.string.character_details_title),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CharacterDetailsIconButton(
            onClick = onEdit,
            icon = Icons.Default.Edit,
            description = stringResource(Res.string.character_edit),
        )
    }
}

@Composable
private fun CharacterDetailsIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CharacterDetailsBody(
    character: Character,
    dispatch: (CharacterDetailsAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(210.dp).background(Color(0x33FFE66D), CircleShape))
                AvatarImage(
                    character.avatar?.url?.value,
                    character.details.name.value,
                    Modifier.size(190.dp).clip(CircleShape).border(BorderStroke(5.dp, Color.White), CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(typeResource(character.details.type)).uppercase() + "  ·  " +
                    stringResource(typeTagResource(character.details.type)).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.1.sp,
                color = DozyColors.textLink,
                textAlign = TextAlign.Center,
            )
            Text(
                character.details.name.value,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 29.sp),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.character_details_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(25.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(23.dp)) {
                DetailsLabel(stringResource(Res.string.personality))
                Text(
                    character.details.personality.value,
                    Modifier.padding(top = 8.dp, bottom = 22.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 25.sp),
                )
                DetailsLabel(stringResource(Res.string.appearance))
                Text(
                    character.details.appearance.value,
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 25.sp),
                )
            }
        }

        Button(
            onClick = { dispatch(CharacterDetailsAction.Story) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Text(stringResource(Res.string.character_story_named, character.details.name.value), fontWeight = FontWeight.Black)
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
        DozySecondaryButton(
            onClick = { dispatch(CharacterDetailsAction.Edit) },
            modifier = Modifier.padding(top = 12.dp).height(54.dp),
        ) {
            Text(stringResource(Res.string.character_edit), fontWeight = FontWeight.Black)
        }
        TextButton(
            onClick = { dispatch(CharacterDetailsAction.Delete) },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 20.dp),
        ) { Text(stringResource(Res.string.character_delete), color = DozyColors.dangerLink) }
    }
}

@Composable
private fun DetailsLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DeleteCharacterDialog(
    state: CharacterDetailsUiState,
    dispatch: (CharacterDetailsAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.deleting) dispatch(CharacterDetailsAction.CancelDelete) },
        title = { Text(stringResource(Res.string.character_delete_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.character_delete_description))
                state.deleteError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                if (state.deleting) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(enabled = !state.deleting, onClick = {
                dispatch(CharacterDetailsAction.ConfirmDelete)
            }) { Text(stringResource(Res.string.character_delete)) }
        },
        dismissButton = {
            TextButton(enabled = !state.deleting, onClick = {
                dispatch(CharacterDetailsAction.CancelDelete)
            }) { Text(stringResource(Res.string.character_keep)) }
        },
    )
}

private fun typeResource(type: Character.Type) =
    when (type) {
        Character.Type.CHILD -> Res.string.child
        Character.Type.ANIMAL -> Res.string.animal
        Character.Type.FANTASY -> Res.string.fantasy
        Character.Type.OTHER -> Res.string.other
    }

private fun typeTagResource(type: Character.Type) =
    when (type) {
        Character.Type.CHILD -> Res.string.character_details_child_tag
        Character.Type.ANIMAL -> Res.string.character_details_animal_tag
        Character.Type.FANTASY -> Res.string.character_details_fantasy_tag
        Character.Type.OTHER -> Res.string.character_details_other_tag
    }
