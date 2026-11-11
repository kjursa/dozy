package com.dozy.mobile.feature.story.presentation.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.R
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.feature.character.domain.model.Character
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.compose.theme.UiSpacing
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
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            StoryScreenGlows()
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                StoryHeader(onBack)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = UiSpacing.x3),
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x4),
                ) {
                    StoryIntro()
                    Row {
                        TextButton(onClick = { dispatch(CreateStoryAction.Automatic(false)) }) {
                            Text(stringResource(R.string.story_own_idea))
                        }
                        TextButton(onClick = { dispatch(CreateStoryAction.Automatic(true)) }) {
                            Text(stringResource(R.string.story_surprise))
                        }
                    }
                    Text(stringResource(if (state.automaticPlot) R.string.story_surprise else R.string.story_own_idea))
                    if (!state.automaticPlot) {
                        Column {
                            Text(stringResource(R.string.place_setting), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.randomSetting) {
                                    stringResource(R.string.place_surprise)
                                } else {
                                    state.placeName
                                        ?: stringResource(R.string.place_selected)
                                },
                            )
                            TextButton(
                                onClick = { dispatch(CreateStoryAction.ChoosePlace) },
                            ) { Text(stringResource(R.string.place_choose)) }
                        }
                    }
                    if (state.characterLoading) CircularProgressIndicator()
                    if (state.characterLoadError) {
                        Text(stringResource(R.string.could_not_load_characters))
                        TextButton(onClick = { dispatch(CreateStoryAction.ReloadCharacters) }) {
                            Text(stringResource(R.string.story_retry_characters))
                        }
                    }
                    HeroPicker(
                        state.heroes,
                        state.selectedHeroes,
                        onToggle = { dispatch(CreateStoryAction.Hero(it)) },
                        onNewHero = onNewHero,
                    )
                    LengthPicker(state.selectedLength, onSelect = { dispatch(CreateStoryAction.Length(it)) })
                    if (!state.automaticPlot) {
                        StoryPrompts(
                            specialRequest = state.specialRequest,
                            onSpecialRequestChange = { dispatch(CreateStoryAction.Request(it)) },
                            ending = state.ending,
                            onEndingChange = { dispatch(CreateStoryAction.Ending(it)) },
                        )
                    }
                    if (!state.characterLoading &&
                        !state.characterLoadError &&
                        state.heroes.isEmpty()
                    ) {
                        Text(stringResource(R.string.story_no_characters))
                    }
                    listOfNotNull(state.castError, state.lengthError, state.ideaError, state.endingError, state.submissionError).forEach {
                        Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(mainBottomMenuScrollSpace()))
                }
            }
            Button(
                onClick = { dispatch(CreateStoryAction.Submit) },
                enabled =
                    state.heroes.isNotEmpty() &&
                        !state.characterLoading &&
                        !state.characterLoadError &&
                        !state.submitting &&
                        state.acceptedJobId == null,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = UiSpacing.x3, vertical = UiSpacing.x3)
                        .height(56.dp)
                        .shadow(16.dp, CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(UiSpacing.x1))
                Text(
                    stringResource(R.string.generate_my_story),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun StoryScreenGlows() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(
                    Alignment.TopStart,
                ).offset(x = (-80).dp, y = (-64).dp)
                .size(210.dp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = .17f), CircleShape),
        )
        Box(
            Modifier
                .align(
                    Alignment.TopEnd,
                ).offset(x = 90.dp, y = 300.dp)
                .size(190.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = .09f), CircleShape),
        )
    }
}

@Composable
private fun StoryHeader(onBack: () -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                val isActive = index < 3
                Box(
                    Modifier
                        .size(width = if (index == 2) 24.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun StoryIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1 / 2)) {
        Text(
            stringResource(R.string.step_1_of_4),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.where_shall_we_go_tonight),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.places_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeroPicker(
    heroes: List<Character>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onNewHero: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Column {
            Text(
                stringResource(R.string.heroes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.who_will_join_the_adventure),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            heroes.forEach { hero ->
                val isSelected = hero.id.value in selected
                Column(
                    modifier = Modifier.clickable { onToggle(hero.id.value) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
                ) {
                    Box {
                        AvatarImage(hero.avatar?.url?.value, hero.details.name.value, Modifier.size(80.dp))
                        if (isSelected) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.selected),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(4.dp),
                                )
                            }
                        }
                    }
                    Text(
                        hero.details.name.value,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier.clickable(onClick = onNewHero),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.new_hero),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LengthPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Column {
            Text(
                stringResource(R.string.length),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.how_long_should_the_story_be),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            LengthCard(
                stringResource(R.string.story_length_short),
                stringResource(R.string.a_little_bedtime_adventure),
                stringResource(R.string.text_3_5_min),
                Icons.Default.Bedtime,
                selected == 0,
                Modifier.weight(1f),
            ) { onSelect(0) }
            LengthCard(
                stringResource(R.string.story_length_long),
                stringResource(R.string.one_more_chapter_of_magic),
                stringResource(R.string.text_8_12_min),
                Icons.Default.AutoAwesome,
                selected == 1,
                Modifier.weight(1f),
            ) { onSelect(1) }
        }
    }
}

@Composable
private fun LengthCard(
    title: String,
    subtitle: String,
    length: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Surface(
        modifier = modifier.shadow(6.dp, MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        Column(Modifier.padding(UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = tint.copy(alpha = .16f)) {
                Box(
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp)) }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                length,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StoryPrompts(
    specialRequest: String,
    onSpecialRequestChange: (String) -> Unit,
    ending: String,
    onEndingChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Column {
            Text(
                stringResource(R.string.optional),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.anything_special_you_d_like),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
            modifier = Modifier.shadow(6.dp, MaterialTheme.shapes.large),
        ) {
            Column(Modifier.padding(UiSpacing.x3), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                StoryInput(
                    value = specialRequest,
                    onValueChange = onSpecialRequestChange,
                    placeholder = stringResource(R.string.maybe_the_heroes_discover_a_hidden_treasure_solve_a),
                    minimumHeight = 96.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                    Text(
                        stringResource(R.string.how_should_it_end_optional),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    StoryInput(
                        value = ending,
                        onValueChange = onEndingChange,
                        placeholder = stringResource(R.string.oliver_finds_the_lost_treasure_and_shares_it_with),
                        minimumHeight = 64.dp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(R.string.leave_empty_and_we_ll_create_a_surprise_ending),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minimumHeight: androidx.compose.ui.unit.Dp,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = minimumHeight).padding(UiSpacing.x2)) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }
    }
}
