package com.dozy.mobile.feature.character.presentation.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.core.presentation.components.CollectionEmptyState
import com.dozy.mobile.core.presentation.components.CollectionScreen
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.add_a_new_character
import com.dozy.mobile.resources.add_character
import com.dozy.mobile.resources.characters_collection_add_title
import com.dozy.mobile.resources.characters_collection_empty_action
import com.dozy.mobile.resources.characters_collection_eyebrow
import com.dozy.mobile.resources.characters_collection_intro
import com.dozy.mobile.resources.characters_collection_title
import com.dozy.mobile.resources.characters_empty_description
import com.dozy.mobile.resources.characters_empty_title
import com.dozy.mobile.resources.characters_offline
import com.dozy.mobile.resources.characters_retry
import com.dozy.mobile.resources.could_not_load_characters
import com.kjursa.foundation.compose.mvi.MviScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CharactersTabScreen(onBack: () -> Unit) {
    val charactersViewModel: CharactersViewModel = koinViewModel()
    MviScreen(
        viewModel = charactersViewModel,
    ) { state, dispatch ->
        CharactersTabContent(state, dispatch, onBack)
    }
}

@Composable
internal fun CharactersTabContent(
    state: CharactersUiState,
    onAction: (CharactersAction) -> Unit,
    onBack: () -> Unit = {},
) {
    val add = { onAction(CharactersAction.AddCharacterClicked) }
    CollectionScreen(
        toolbarTitle = stringResource(Res.string.characters_collection_title),
        eyebrow = stringResource(Res.string.characters_collection_eyebrow),
        title = stringResource(Res.string.characters_collection_title),
        intro = stringResource(Res.string.characters_collection_intro),
        addDescription = stringResource(Res.string.add_character),
        onBack = onBack,
        onRefresh = { onAction(CharactersAction.Refresh) },
        refreshing = state.refreshing,
        onAdd = add,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) CircularProgressIndicator()
            if (state.characterLoadError) {
                Text(stringResource(Res.string.could_not_load_characters), color = MaterialTheme.colorScheme.error)
            }
            if (state.offline) Text(stringResource(Res.string.characters_offline))
            if (state.characterLoadError || state.offline) {
                TextButton(enabled = !state.refreshing, onClick = { onAction(CharactersAction.Refresh) }) {
                    Text(stringResource(Res.string.characters_retry))
                }
            }
            if (!state.loading && !state.refreshing && !state.characterLoadError && state.characters.isEmpty()) {
                CollectionEmptyState(
                    Icons.Default.Face,
                    stringResource(Res.string.characters_empty_title),
                    stringResource(Res.string.characters_empty_description),
                    stringResource(Res.string.characters_collection_empty_action),
                    add,
                )
            }
            if (state.characters.isNotEmpty()) {
                val tiles = state.characters + listOf(null)
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { character ->
                            if (character == null) {
                                AddCharacterTile(Modifier.weight(1f), add)
                            } else {
                                CharacterLibraryCard(character, Modifier.weight(1f)) {
                                    onAction(CharactersAction.CharacterSelected(character))
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterLibraryCard(character: CharacterListItem, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(27.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 218.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AvatarImage(
                character.avatarUrl,
                character.name,
                Modifier.size(106.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                character.name,
                Modifier.padding(top = 15.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center,
            )
            TileCaption(character.description)
        }
    }
}

@Composable
private fun AddCharacterTile(modifier: Modifier, onAdd: () -> Unit) {
    val shape = RoundedCornerShape(27.dp)
    Column(
        modifier.heightIn(min = 218.dp).clip(shape).clickable(role = Role.Button, onClick = onAdd)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFEBDCC7),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(27.dp.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }.padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Add, null, Modifier.size(48.dp), tint = Color(0xFF4B8C7D))
        Text(
            stringResource(Res.string.characters_collection_add_title),
            Modifier.padding(top = 15.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center,
        )
        TileCaption(stringResource(Res.string.add_a_new_character))
    }
}

@Composable
private fun TileCaption(text: String) {
    Text(
        text,
        Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
