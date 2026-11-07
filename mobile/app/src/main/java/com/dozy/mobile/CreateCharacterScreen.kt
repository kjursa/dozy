package com.dozy.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kjursa.composeuikit.theme.UiSpacing
import com.kjursa.composeuikit.mvi.MviScreen

private data class HeroType(val label: String, val icon: ImageVector)

private val heroTypes = listOf(
    HeroType("Child", Icons.Default.Face),
    HeroType("Animal", Icons.Default.Pets),
    HeroType("Fantasy", Icons.Default.AutoAwesome),
    HeroType("Other", Icons.Default.Stars),
)

@Composable
internal fun CreateCharacterScreen(
    viewModel: CreateCharacterViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                CreateCharacterEffect.NavigateBack -> onBack()
                CreateCharacterEffect.SaveRequested -> onSaved()
                CreateCharacterEffect.PreviewRequested -> Unit
            }
        },
    ) { state, dispatch ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            CharacterScreenGlows()
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                CharacterHeader { dispatch(CreateCharacterAction.BackClicked) }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = UiSpacing.x3),
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x3),
                ) {
                    CharacterPreview(state.name)
                    HeroTypePicker(state.selectedType) { dispatch(CreateCharacterAction.TypeSelected(it)) }
                    CharacterDetails(
                        name = state.name,
                        onNameChange = { dispatch(CreateCharacterAction.NameChanged(it)) },
                        personality = state.personality,
                        onPersonalityChange = { dispatch(CreateCharacterAction.PersonalityChanged(it)) },
                        appearance = state.appearance,
                        onAppearanceChange = { dispatch(CreateCharacterAction.AppearanceChanged(it)) },
                    )
                    Spacer(Modifier.height(mainBottomMenuScrollSpace()))
                }
            }
            CharacterActions(
                modifier = Modifier.align(Alignment.BottomCenter),
                onPreview = { dispatch(CreateCharacterAction.PreviewClicked) },
                onSave = { dispatch(CreateCharacterAction.SaveClicked) },
            )
        }
    }
}

@Composable
private fun CharacterScreenGlows() {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 80.dp, y = (-70).dp).size(220.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .08f), CircleShape))
        Box(Modifier.align(Alignment.TopStart).offset(x = (-90).dp, y = 420.dp).size(200.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = .08f), CircleShape))
    }
}

@Composable
private fun CharacterHeader(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = UiSpacing.x3, vertical = UiSpacing.x2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp).shadow(6.dp, CircleShape).clickable(onClick = onBack),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.secondary)
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = .8f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        ) {
            Row(Modifier.padding(horizontal = UiSpacing.x2, vertical = UiSpacing.x1), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("New Character", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun CharacterPreview(name: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
        Box(Modifier.size(256.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.padding(UiSpacing.x3).fillMaxSize().background(MaterialTheme.colorScheme.tertiary.copy(alpha = .28f), CircleShape))
            Image(
                painterResource(R.drawable.character_oliver),
                contentDescription = "$name preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = UiSpacing.x1, bottom = UiSpacing.x1).shadow(5.dp, CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            ) {
                Text("Preview", modifier = Modifier.padding(horizontal = UiSpacing.x2, vertical = UiSpacing.x1), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Text(name.ifBlank { "Your hero" }, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Curious explorer, age 5", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeroTypePicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Text("WHO IS THIS HERO?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            heroTypes.forEach { type ->
                val isSelected = type.label == selected
                val typeShape = MaterialTheme.shapes.medium
                Surface(
                    modifier = Modifier.weight(1f).clip(typeShape).clickable { onSelect(type.label) },
                    shape = typeShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        Modifier.padding(vertical = UiSpacing.x2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
                    ) {
                        Icon(type.icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Text(type.label, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CharacterDetails(
    name: String,
    onNameChange: (String) -> Unit,
    personality: String,
    onPersonalityChange: (String) -> Unit,
    appearance: String,
    onAppearanceChange: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        modifier = Modifier.shadow(6.dp, MaterialTheme.shapes.large),
    ) {
        Column(Modifier.padding(UiSpacing.x3), verticalArrangement = Arrangement.spacedBy(UiSpacing.x3)) {
            Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text("Character name", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                CharacterInput(value = name, onValueChange = onNameChange, singleLine = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text("Personality", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                CharacterInput(value = personality, onValueChange = onPersonalityChange, minimumHeight = 96.dp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                    listOf("Curious", "Brave", "Kind", "Funny").forEach { TraitChip(it) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text("Appearance", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                CharacterInput(value = appearance, onValueChange = onAppearanceChange, minimumHeight = 112.dp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                    AppearanceChip("Yellow hoodie", MaterialTheme.colorScheme.tertiary)
                    AppearanceChip("Green eyes", MaterialTheme.colorScheme.secondary)
                    AppearanceChip("Brown curls", MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun CharacterInput(value: String, onValueChange: (String) -> Unit, singleLine: Boolean = false, minimumHeight: androidx.compose.ui.unit.Dp = 48.dp) {
    Surface(
        shape = if (singleLine) CircleShape else MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = minimumHeight).padding(horizontal = UiSpacing.x2, vertical = UiSpacing.x1), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }
    }
}

@Composable
private fun TraitChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = UiSpacing.x1, vertical = UiSpacing.x1 / 2), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppearanceChip(label: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = UiSpacing.x1, vertical = UiSpacing.x1 / 2), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1 / 2)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CharacterActions(modifier: Modifier, onPreview: () -> Unit, onSave: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = UiSpacing.x3).navigationBarsPadding()
            .padding(bottom = UiSpacing.x3).shadow(18.dp, RoundedCornerShape(40.dp)),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Row(Modifier.padding(UiSpacing.x1), horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(UiSpacing.x1))
                Text("Generate Preview", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(UiSpacing.x1))
                Text("Save Character", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
