package com.dozy.mobile.feature.character.presentation.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dozy.mobile.R
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.feature.character.domain.model.Character
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.compose.theme.UiSpacing
import com.kjursa.foundation.presentation.text.UiText
import com.kjursa.foundation.presentation.text.resolve

private data class HeroType(
    val value: String,
    val label: Int,
    val icon: ImageVector,
)

private val heroTypes =
    listOf(
        HeroType("Child", R.string.child, Icons.Default.Face),
        HeroType("Animal", R.string.animal, Icons.Default.Pets),
        HeroType("Fantasy", R.string.fantasy, Icons.Default.AutoAwesome),
        HeroType("Other", R.string.other, Icons.Default.Stars),
    )

@Composable
internal fun CreateCharacterScreen(
    viewModel: CreateCharacterViewModel,
    onBack: () -> Unit,
    onSaved: (Character.Id) -> Unit,
) {
    BackHandler { viewModel.dispatch(CreateCharacterAction.BackClicked) }
    DisposableEffect(viewModel) {
        viewModel.resume()
        onDispose { viewModel.abandon() }
    }
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                CreateCharacterEffect.NavigateBack -> onBack()
                is CreateCharacterEffect.Saved -> onSaved(effect.id)
            }
        },
    ) { state, dispatch ->
        val name = state.displayName.resolve()
        val personality = state.displayPersonality.resolve()
        val appearance = state.displayAppearance.resolve()
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            CharacterScreenGlows()
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                CharacterHeader(state.editingId != null) { dispatch(CreateCharacterAction.BackClicked) }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = UiSpacing.x3),
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x3),
                ) {
                    if (state.loading) CircularProgressIndicator()
                    if (state.unavailable) Text(stringResource(R.string.character_unavailable))
                    state.loadError?.let {
                        Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { dispatch(CreateCharacterAction.RetryLoadClicked) }) {
                            Text(stringResource(R.string.characters_retry))
                        }
                    }
                    CharacterPreview(name, state.avatarUrl)
                    if (state.avatarOutdated) Text(stringResource(R.string.avatar_earlier_description))
                    if (state.editingId == null && name.isBlank()) Text(stringResource(R.string.character_inspiration))
                    state.generationError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                    HeroTypePicker(
                        state.selectedType,
                        !state.busy && !state.unavailable && state.loadError == null,
                    ) { dispatch(CreateCharacterAction.TypeSelected(it)) }
                    state.typeError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                    CharacterDetails(
                        enabled = !state.busy && !state.unavailable && state.loadError == null,
                        nameError = state.nameError,
                        personalityError = state.personalityError,
                        appearanceError = state.appearanceError,
                        name = name,
                        onNameChange = { dispatch(CreateCharacterAction.NameChanged(it)) },
                        personality = personality,
                        onPersonalityChange = { dispatch(CreateCharacterAction.PersonalityChanged(it)) },
                        appearance = appearance,
                        onAppearanceChange = { dispatch(CreateCharacterAction.AppearanceChanged(it)) },
                    )
                    when (state.saveStatus) {
                        SaveStatus.SAVING -> Text(stringResource(R.string.saving_character))
                        SaveStatus.FAILURE ->
                            Text(
                                stringResource(R.string.character_save_failed),
                                color = MaterialTheme.colorScheme.error,
                            )
                        SaveStatus.EDITING -> Unit
                    }
                    Spacer(Modifier.height(mainBottomMenuScrollSpace()))
                }
            }
            CharacterActions(
                modifier = Modifier.align(Alignment.BottomCenter),
                onPreview = { dispatch(CreateCharacterAction.PreviewClicked) },
                onSave = { dispatch(CreateCharacterAction.SaveClicked) },
                saving = state.saveStatus == SaveStatus.SAVING,
                generating = state.generating,
                canSave = state.canSave,
                canGenerate = !state.busy && !state.unavailable && state.loadError == null,
                hasAvatar = state.avatarId != null,
            )
        }
    }
}

@Composable
private fun CharacterScreenGlows() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(
                    Alignment.TopEnd,
                ).offset(x = 80.dp, y = (-70).dp)
                .size(220.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .08f), CircleShape),
        )
        Box(
            Modifier
                .align(
                    Alignment.TopStart,
                ).offset(x = (-90).dp, y = 420.dp)
                .size(200.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = .08f), CircleShape),
        )
    }
}

@Composable
private fun CharacterHeader(
    editing: Boolean,
    onBack: () -> Unit,
) {
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = .8f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        ) {
            Row(
                Modifier.padding(horizontal = UiSpacing.x2, vertical = UiSpacing.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1),
            ) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(if (editing) R.string.character_edit else R.string.new_character),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun CharacterPreview(
    name: String,
    avatarUrl: String?,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AvatarImage(avatarUrl, name, Modifier.size(220.dp))
        Text(name.ifBlank { stringResource(R.string.your_hero) }, style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.avatar_description_hint))
    }
}

@Composable
private fun HeroTypePicker(
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Text(
            stringResource(R.string.who_is_this_hero),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            heroTypes.forEach { type ->
                val isSelected = type.value == selected
                val typeShape = MaterialTheme.shapes.medium
                Surface(
                    modifier = Modifier.weight(1f).clip(typeShape).clickable(enabled = enabled) { onSelect(type.value) },
                    shape = typeShape,
                    color = MaterialTheme.colorScheme.surface,
                    border =
                        BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                ) {
                    Column(
                        Modifier.padding(vertical = UiSpacing.x2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
                    ) {
                        Icon(
                            type.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            stringResource(type.label),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CharacterDetails(
    enabled: Boolean,
    nameError: UiText?,
    personalityError: UiText?,
    appearanceError: UiText?,
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
                Text(
                    stringResource(R.string.character_name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CharacterInput(
                    enabled = enabled,
                    placeholder = stringResource(R.string.character_name_placeholder),
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                )
                nameError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(
                    stringResource(R.string.personality),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CharacterInput(
                    enabled = enabled,
                    placeholder = stringResource(R.string.character_personality_placeholder),
                    value = personality,
                    onValueChange = onPersonalityChange,
                    minimumHeight = 96.dp,
                )
                personalityError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(
                    stringResource(R.string.appearance),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CharacterInput(
                    enabled = enabled,
                    placeholder = stringResource(R.string.character_appearance_placeholder),
                    value = appearance,
                    onValueChange = onAppearanceChange,
                    minimumHeight = 112.dp,
                )
                appearanceError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun CharacterInput(
    enabled: Boolean,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    minimumHeight: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Surface(
        shape = if (singleLine) CircleShape else MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = minimumHeight).padding(horizontal = UiSpacing.x2, vertical = UiSpacing.x1),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                enabled = enabled,
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
private fun CharacterActions(
    modifier: Modifier,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
    generating: Boolean,
    canSave: Boolean,
    canGenerate: Boolean,
    hasAvatar: Boolean,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = UiSpacing.x3)
                .navigationBarsPadding()
                .padding(bottom = UiSpacing.x3)
                .shadow(18.dp, RoundedCornerShape(40.dp)),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Row(Modifier.padding(UiSpacing.x1), horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            OutlinedButton(
                onClick = onPreview,
                enabled = canGenerate,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                if (generating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(if (hasAvatar) R.string.avatar_regenerate else R.string.avatar_generate))
                }
            }
            if (saving) {
                Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(UiSpacing.x1))
                    Text(
                        stringResource(R.string.save_character),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
