package com.dozy.mobile.feature.character.presentation.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dozy.mobile.core.presentation.components.AmbientGlow
import com.dozy.mobile.core.presentation.components.Eyebrow
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.kjursa.composeuikit.mvi.MviScreen
import com.kjursa.composeuikit.theme.UiSpacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CharactersTabScreen(onAddCharacter: () -> Unit) {
    val charactersViewModel: CharactersViewModel = koinViewModel()
    MviScreen(
        viewModel = charactersViewModel,
        onEffect = { effect ->
            when (effect) {
                is CharactersEffect.OpenCharacter -> Unit // A character detail destination does not exist yet.
                CharactersEffect.NavigateToAddCharacter -> onAddCharacter()
            }
        },
    ) { state, dispatch ->
        Column {
            if (state.characterLoadError) Text("Could not load characters.", color = MaterialTheme.colorScheme.error)
            CharactersTabContent(state, dispatch)
        }
    }
}

@Composable
internal fun CharactersTabContent(
    state: CharactersUiState,
    onAction: (CharactersAction) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = UiSpacing.x3),
                verticalArrangement = Arrangement.spacedBy(UiSpacing.x4),
            ) {
                Column(Modifier.padding(top = UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                    Eyebrow(state.eyebrow)
                    Text(state.title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        state.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.characters.chunked(2).forEach { rowCharacters ->
                    Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                        rowCharacters.forEach { character ->
                            CharacterLibraryCard(character, Modifier.weight(1f)) { onAction(CharactersAction.CharacterSelected(character)) }
                        }
                        if (rowCharacters.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                CharacterLibraryCard(onAddCharacter = { onAction(CharactersAction.AddCharacterClicked) })
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
private fun CharacterLibraryCard(
    character: CharacterListItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.shadow(6.dp, MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Column(Modifier.padding(UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            Image(
                painterResource(character.image),
                contentDescription = character.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Text(character.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CharacterLibraryCard(onAddCharacter: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAddCharacter),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(UiSpacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2),
        ) {
            Icon(
                Icons.Default.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text("Add a new character", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Create another hero for your stories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
