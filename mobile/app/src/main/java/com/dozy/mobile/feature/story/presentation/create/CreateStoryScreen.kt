package com.dozy.mobile.feature.story.presentation.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.R
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.feature.character.domain.model.Character
import com.dozy.mobile.ui.theme.DozyColors
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.UiText
import com.kjursa.foundation.presentation.text.resolve

@Composable
internal fun CreateStoryScreen(
    onBack: () -> Unit,
    onNewHero: () -> Unit,
    onGenerate: (String) -> Unit,
    onChoosePlace: () -> Unit,
    viewModel: CreateStoryViewModel,
) {
    LaunchedEffect(viewModel) { viewModel.resumed() }
    MviScreen(viewModel, onEffect = { effect ->
        when (effect) {
            CreateStoryEffect.OpenPlaces -> onChoosePlace()
            is CreateStoryEffect.Accepted -> onGenerate(effect.jobId)
        }
    }) { state, dispatch ->
        CreateStoryContent(state, dispatch, onBack, onNewHero)
    }
}

@Composable
internal fun CreateStoryContent(
    state: CreateStoryUiState,
    dispatch: (CreateStoryAction) -> Unit,
    onBack: () -> Unit,
    onNewHero: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.secondary.copy(alpha = .07f), Color.Transparent),
                        Offset(size.width, 0f),
                        size.width,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.primary.copy(alpha = .04f), Color.Transparent),
                        Offset(0f, size.height * .18f),
                        size.width,
                    ),
                )
            }.statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        StoryHeader(onBack)
        Column(Modifier.fillMaxWidth().padding(start = 25.dp, end = 25.dp, top = 8.dp, bottom = 35.dp)) {
            StoryIntro()
            HeroPicker(state, dispatch, onNewHero)
            Spacer(Modifier.height(28.dp))
            StoryModePicker(state.automaticPlot) { dispatch(CreateStoryAction.Automatic(it)) }
            Spacer(Modifier.height(28.dp))
            if (state.automaticPlot) {
                AutomaticStoryCard()
            } else {
                StoryPlacePicker(state) { dispatch(CreateStoryAction.ChoosePlace) }
                Spacer(Modifier.height(28.dp))
                StoryPrompts(state, dispatch)
            }
            LengthPicker(state.selectedLength) { dispatch(CreateStoryAction.Length(it)) }
            FieldError(state.lengthError)
            Surface(
                color = DozyColors.informationBackground,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(vertical = 24.dp),
            ) {
                Text(
                    stringResource(R.string.story_cover_hint),
                    Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 22.sp),
                    color = colors.onSurfaceVariant,
                )
            }
            FieldError(state.submissionError)
            Button(
                onClick = { dispatch(CreateStoryAction.Submit) },
                enabled =
                    state.heroes.isNotEmpty() &&
                        !state.characterLoading &&
                        !state.characterLoadError &&
                        !state.submitting &&
                        state.acceptedJobId == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Text(
                    stringResource(R.string.generate_my_story),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                )
                Spacer(Modifier.width(14.dp))
                if (state.submitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(20.dp))
                }
            }
            Text(
                stringResource(R.string.story_submit_hint),
                Modifier.fillMaxWidth().padding(top = 13.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal, lineHeight = 19.sp),
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StoryHeader(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.back),
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Text(
            stringResource(R.string.story_form_title),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.5.sp),
        )
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun StoryIntro() {
    Text(
        stringResource(R.string.story_form_eyebrow),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 17.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        stringResource(R.string.story_form_heading),
        Modifier.padding(top = 12.dp),
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1.3).sp),
    )
    Text(
        stringResource(R.string.story_form_intro),
        Modifier.padding(top = 16.dp, bottom = 28.dp),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-.3).sp),
        modifier = Modifier.padding(bottom = 15.dp),
    )
}

@Composable
private fun Hint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FieldError(error: UiText?) {
    if (error !=
        null
    ) {
        Text(
            error.resolve(),
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun HeroPicker(
    state: CreateStoryUiState,
    dispatch: (CreateStoryAction) -> Unit,
    onNewHero: () -> Unit,
) {
    SectionTitle(stringResource(R.string.story_cast_title))
    Hint(stringResource(R.string.story_cast_hint), Modifier.padding(bottom = 13.dp))
    when {
        state.characterLoading -> CircularProgressIndicator(Modifier.size(28.dp))
        state.characterLoadError -> {
            Text(stringResource(R.string.could_not_load_characters), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { dispatch(CreateStoryAction.ReloadCharacters) }) { Text(stringResource(R.string.story_retry_characters)) }
        }
        state.heroes.isEmpty() -> EmptyCastCard(onNewHero)
        else -> {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 5.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                state.heroes.forEach { hero ->
                    HeroChoice(hero, hero.id.value in state.selectedHeroes) { dispatch(CreateStoryAction.Hero(hero.id.value)) }
                }
            }
            TextButton(onClick = onNewHero, contentPadding = PaddingValues(vertical = 12.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.story_add_character), color = DozyColors.textLink, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    FieldError(state.castError)
}

@Composable
private fun HeroChoice(
    hero: Character,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(92.dp).selectable(selected, role = Role.Checkbox, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(76.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(
                        3.dp,
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape,
                    ).padding(
                        3.dp,
                    ).border(
                        3.dp,
                        MaterialTheme.colorScheme.onPrimary,
                        CircleShape,
                    ).padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AvatarImage(hero.avatar?.url?.value, hero.details.name.value, Modifier.fillMaxSize())
            }
            if (selected) {
                Surface(
                    Modifier.align(Alignment.TopEnd).size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                ) {
                    Icon(Icons.Default.Check, null, Modifier.padding(3.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Text(
            hero.details.name.value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyCastCard(onNewHero: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DozyColors.emptyStateBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(30.dp), tint = DozyColors.inspirationAccent)
            Text(
                stringResource(R.string.story_empty_cast_title),
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Hint(stringResource(R.string.story_empty_cast_description), Modifier.padding(vertical = 12.dp))
            FilledTonalButton(onClick = onNewHero) { Text(stringResource(R.string.story_create_character)) }
        }
    }
}

@Composable
private fun StoryModePicker(
    automatic: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    SectionTitle(stringResource(R.string.story_mode_title))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard(
            stringResource(R.string.story_own_idea),
            stringResource(R.string.story_manual_hint),
            !automatic,
            Modifier.fillMaxWidth(),
        ) {
            onSelect(false)
        }
        ChoiceCard(
            stringResource(R.string.story_surprise),
            stringResource(R.string.story_automatic_hint),
            automatic,
            Modifier.fillMaxWidth(),
        ) {
            onSelect(true)
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) DozyColors.selectedBackground else MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) DozyColors.selectedBorder else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Row(
            Modifier.padding(horizontal = if (compact) 9.dp else 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
        ) {
            RadioButton(
                selected,
                onClick = null,
                modifier = Modifier.size(20.dp),
                colors = RadioButtonDefaults.colors(selectedColor = DozyColors.selectionControl),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = if (compact) 12.sp else 14.sp))
                Text(
                    subtitle,
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StoryPlacePicker(
    state: CreateStoryUiState,
    onChoose: () -> Unit,
) {
    SectionTitle(stringResource(R.string.story_setting_title))
    Surface(
        onClick = onChoose,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Surface(shape = RoundedCornerShape(15.dp), color = DozyColors.settingIconBackground, modifier = Modifier.size(43.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(25.dp), tint = DozyColors.settingIcon) }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.randomSetting) {
                        stringResource(R.string.story_surprise)
                    } else {
                        state.placeName
                            ?: stringResource(R.string.place_selected)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Hint(
                    stringResource(if (state.randomSetting) R.string.story_random_place_hint else R.string.story_change_place_hint),
                    Modifier.padding(top = 6.dp),
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp), tint = DozyColors.navigationAccent)
        }
    }
    Hint(stringResource(R.string.story_setting_hint), Modifier.padding(top = 7.dp))
}

@Composable
private fun StoryPrompts(
    state: CreateStoryUiState,
    dispatch: (CreateStoryAction) -> Unit,
) {
    StoryInput(
        stringResource(R.string.story_idea_label),
        state.specialRequest,
        stringResource(R.string.story_idea_placeholder),
        state.ideaError,
        120.dp,
    ) {
        dispatch(CreateStoryAction.Request(it))
    }
    Spacer(Modifier.height(25.dp))
    StoryInput(
        stringResource(R.string.story_ending_label),
        state.ending,
        stringResource(R.string.story_ending_placeholder),
        state.endingError,
        90.dp,
    ) {
        dispatch(CreateStoryAction.Ending(it))
    }
    Spacer(Modifier.height(25.dp))
}

@Composable
private fun StoryInput(
    label: String,
    value: String,
    placeholder: String,
    error: UiText?,
    minimumHeight: Dp,
    onValueChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.story_optional),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 27.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        modifier =
            Modifier
                .semantics {
                    contentDescription = label
                }.padding(
                    top = 13.dp,
                ).fillMaxWidth()
                .heightIn(
                    min = minimumHeight,
                ).clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    if (error ==
                        null
                    ) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    RoundedCornerShape(22.dp),
                ).padding(19.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 27.sp,
                            ),
                        color = DozyColors.placeholder,
                    )
                }
                inner()
            }
        },
    )
    FieldError(error)
}

@Composable
private fun AutomaticStoryCard() {
    Surface(shape = RoundedCornerShape(26.dp), color = DozyColors.inspirationBackground, modifier = Modifier.padding(bottom = 28.dp)) {
        Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(42.dp), tint = DozyColors.inspirationAccent)
            Text(
                stringResource(R.string.story_automatic_title),
                Modifier.padding(vertical = 14.dp),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.story_automatic_description),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 23.sp),
                color = DozyColors.onInspiration,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LengthPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    SectionTitle(stringResource(R.string.story_length_title))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard(
            stringResource(R.string.story_short_label),
            stringResource(R.string.text_3_5_min),
            selected == 0,
            Modifier.weight(1f),
            compact = true,
        ) {
            onSelect(0)
        }
        ChoiceCard(
            stringResource(R.string.story_long_label),
            stringResource(R.string.text_8_12_min),
            selected == 1,
            Modifier.weight(1f),
            compact = true,
        ) {
            onSelect(1)
        }
    }
}
