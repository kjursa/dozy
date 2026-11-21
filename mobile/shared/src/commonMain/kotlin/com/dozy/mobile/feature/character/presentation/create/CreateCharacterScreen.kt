@file:OptIn(ExperimentalComposeUiApi::class)

package com.dozy.mobile.feature.character.presentation.create

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.resources.*
import com.dozy.mobile.ui.common.DozyActionButton
import com.dozy.mobile.ui.common.DozyFormField
import com.dozy.mobile.ui.common.DozyLayout
import com.dozy.mobile.ui.common.DozySecondaryButton
import com.dozy.mobile.ui.theme.DozyColors
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.compose.theme.UiSpacing
import com.kjursa.foundation.presentation.text.UiText
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class HeroType(
    val value: String,
    val label: StringResource,
    val icon: ImageVector,
)

private val heroTypes =
    listOf(
        HeroType("Child", Res.string.child, Icons.Default.Face),
        HeroType("Animal", Res.string.animal, Icons.Default.Pets),
        HeroType("Fantasy", Res.string.fantasy, Icons.Default.AutoAwesome),
        HeroType("Other", Res.string.other, Icons.Default.Stars),
    )

@Composable
internal fun CreateCharacterScreen(viewModel: CreateCharacterViewModel) {
    BackHandler { viewModel.dispatch(CreateCharacterAction.BackClicked) }
    DisposableEffect(viewModel) {
        viewModel.resume()
        onDispose { viewModel.abandon() }
    }
    MviScreen(
        viewModel = viewModel,
    ) { state, dispatch ->
        val draft = state.draft
        val editable = state as? CreateCharacterUiState.Editable
        val name = draft.displayName.resolve()
        val personality = draft.displayPersonality.resolve()
        val appearance = draft.displayAppearance.resolve()
        Box(Modifier.fillMaxSize().imePadding().background(MaterialTheme.colorScheme.background)) {
            CharacterScreenGlows()
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                CharacterHeader(draft.editingId != null) { dispatch(CreateCharacterAction.BackClicked) }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = UiSpacing.x3),
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x3),
                ) {
                    if (state is CreateCharacterUiState.Loading) CircularProgressIndicator()
                    if (state is CreateCharacterUiState.Unavailable) Text(stringResource(Res.string.character_unavailable))
                    (state as? CreateCharacterUiState.LoadFailed)?.error?.let {
                        Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { dispatch(CreateCharacterAction.RetryLoadClicked) }) {
                            Text(stringResource(Res.string.characters_retry))
                        }
                    }
                    CharacterPreview(name, draft.avatarUrl)
                    if (draft.avatarOutdated) Text(stringResource(Res.string.avatar_earlier_description))
                    if (draft.editingId == null && name.isBlank()) Text(stringResource(Res.string.character_inspiration))
                    editable?.generationError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                    HeroTypePicker(
                        draft.selectedType,
                        editable != null,
                    ) { dispatch(CreateCharacterAction.TypeSelected(it)) }
                    draft.typeError?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                    CharacterDetails(
                        enabled = editable != null,
                        nameError = draft.nameError,
                        personalityError = draft.personalityError,
                        appearanceError = draft.appearanceError,
                        name = name,
                        onNameChange = { dispatch(CreateCharacterAction.NameChanged(it)) },
                        personality = personality,
                        onPersonalityChange = { dispatch(CreateCharacterAction.PersonalityChanged(it)) },
                        appearance = appearance,
                        onAppearanceChange = { dispatch(CreateCharacterAction.AppearanceChanged(it)) },
                    )
                    when (state) {
                        is CreateCharacterUiState.Saving -> Text(stringResource(Res.string.saving_character))
                        is CreateCharacterUiState.AvatarReady -> if (state.saveFailed) {
                            Text(stringResource(Res.string.character_save_failed), color = MaterialTheme.colorScheme.error)
                        }
                        else -> Unit
                    }
                    Text(stringResource(Res.string.avatar_description_hint), style = MaterialTheme.typography.bodySmall)
                    val onPreview: (() -> Unit)? =
                        if (state is CreateCharacterUiState.CanGenerate) {
                            { dispatch(CreateCharacterAction.PreviewClicked) }
                        } else {
                            null
                        }
                    val onSave: (() -> Unit)? =
                        if (state is CreateCharacterUiState.AvatarReady) {
                            { dispatch(CreateCharacterAction.SaveClicked) }
                        } else {
                            null
                        }
                    CharacterActions(
                        onPreview = onPreview,
                        onSave = onSave,
                        saving = state is CreateCharacterUiState.Saving,
                        generating = state is CreateCharacterUiState.Generating,
                        hasAvatar = draft.avatarId != null,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CharacterScreenGlows() {
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.radialGradient(listOf(DozyColors.brandTeal.copy(alpha = .075f), Color.Transparent), Offset(size.width, size.height * .03f), size.width))
            drawRect(Brush.radialGradient(listOf(DozyColors.brandCoral.copy(alpha = .043f), Color.Transparent), Offset(0f, size.height * .18f), size.width * .8f))
        },
    )
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
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            stringResource(if (editing) Res.string.character_edit else Res.string.new_character).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun CharacterPreview(
    name: String,
    avatarUrl: String?,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.padding(top = 15.dp, bottom = 25.dp).size(190.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(5.dp, MaterialTheme.colorScheme.surface),
        ) {
            if (avatarUrl != null) {
                AvatarImage(avatarUrl, name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.Face, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(Res.string.your_hero), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text(
            stringResource(Res.string.character_preview_eyebrow),
            style = MaterialTheme.typography.labelSmall,
            color = DozyColors.textLink,
        )
        Text(
            name.ifBlank { stringResource(Res.string.character_preview_title) },
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 29.sp),
        )
        Text(
            stringResource(Res.string.character_preview_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            stringResource(Res.string.who_is_this_hero),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            heroTypes.forEach { type ->
                val isSelected = type.value == selected
                val typeShape = RoundedCornerShape(20.dp)
                Surface(
                    modifier = Modifier.weight(1f).clip(typeShape).clickable(enabled = enabled) { onSelect(type.value) },
                    shape = typeShape,
                    color = MaterialTheme.colorScheme.surface,
                    border =
                        BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) DozyColors.selectionControl else MaterialTheme.colorScheme.outline,
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(DozyLayout.cardPadding), verticalArrangement = Arrangement.spacedBy(DozyLayout.contentGap)) {
            DozyFormField(
                label = stringResource(Res.string.character_name),
                enabled = enabled,
                placeholder = stringResource(Res.string.character_name_placeholder),
                value = name,
                onValueChange = onNameChange,
                error = nameError?.resolve(),
                singleLine = true,
            )
            DozyFormField(
                label = stringResource(Res.string.personality),
                enabled = enabled,
                placeholder = stringResource(Res.string.character_personality_placeholder),
                value = personality,
                onValueChange = onPersonalityChange,
                error = personalityError?.resolve(),
                minimumHeight = 96.dp,
            )
            DozyFormField(
                label = stringResource(Res.string.appearance),
                enabled = enabled,
                placeholder = stringResource(Res.string.character_appearance_placeholder),
                value = appearance,
                onValueChange = onAppearanceChange,
                error = appearanceError?.resolve(),
                minimumHeight = 112.dp,
            )
        }
    }
}

@Composable
private fun CharacterActions(
    onPreview: (() -> Unit)?,
    onSave: (() -> Unit)?,
    saving: Boolean,
    generating: Boolean,
    hasAvatar: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DozySecondaryButton(onClick = { onPreview?.invoke() }, enabled = onPreview != null) {
            if (generating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(if (hasAvatar) Res.string.avatar_regenerate else Res.string.avatar_generate), fontWeight = FontWeight.Black)
                Icon(Icons.Default.Stars, null, Modifier.padding(start = 8.dp).size(20.dp))
            }
        }
        DozyActionButton(onClick = { onSave?.invoke() }, enabled = onSave != null) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(Res.string.save_character), fontWeight = FontWeight.Black)
                Icon(Icons.Default.CheckCircle, null, Modifier.padding(start = 8.dp).size(20.dp))
            }
        }
    }
}
