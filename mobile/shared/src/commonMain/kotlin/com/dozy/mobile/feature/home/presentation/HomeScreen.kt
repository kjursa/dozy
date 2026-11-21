package com.dozy.mobile.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.core.presentation.components.SectionHeading
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.feature.place.presentation.list.PlaceCard
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.feature.story.presentation.components.StoryWideCard
import com.dozy.mobile.feature.story.presentation.generation.GenerationAction
import com.dozy.mobile.feature.story.presentation.generation.message
import com.dozy.mobile.feature.story.presentation.list.Story
import com.dozy.mobile.resources.*
import com.dozy.mobile.ui.common.DozyActionButton
import com.dozy.mobile.ui.common.DozyLayout
import com.dozy.mobile.ui.common.DozyTextStyles
import com.dozy.mobile.ui.theme.DozyColors
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.compose.theme.UiSpacing
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen() {
    val homeViewModel: HomeViewModel = koinViewModel()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { homeViewModel.resumed() }
    MviScreen(
        viewModel = homeViewModel,
    ) { state, dispatch ->
        HomeContent(state, dispatch)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HomeBackground()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader()
            Column(
                Modifier.fillMaxWidth().padding(horizontal = DozyLayout.screenGutter),
                verticalArrangement = Arrangement.spacedBy(DozyLayout.sectionGap),
            ) {
                if (state.generation.homeJob != null || HomeSection.GENERATION in state.errors) {
                    HomeJob(state, onAction)
                }
                CreateStoryBanner { onAction(HomeAction.CreateStoryClicked) }
                Column {
                    HomeSectionStatus(state, HomeSection.STORIES, onAction)
                    StoriesSection(state.stories, onStorySelected = {
                        onAction(HomeAction.StorySelected(it))
                    }, onSeeAll = { onAction(HomeAction.SeeAllStoriesClicked) })
                    if (HomeSection.STORIES !in state.loading &&
                        HomeSection.STORIES !in state.errors &&
                        state.stories.isEmpty() &&
                        state.characters.isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.home_first_story), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column {
                    HomeSectionStatus(state, HomeSection.CHARACTERS, onAction)
                    CharactersSection(
                        state.characters,
                        onAddCharacter = { onAction(HomeAction.AddCharacterClicked) },
                        onCharacterSelected = { onAction(HomeAction.CharacterSelected(it)) },
                        onSeeAll = { onAction(HomeAction.SeeAllCharactersClicked) },
                    )
                }
                HomePlaces(state, onAction)
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
private fun HomeBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.radialGradient(
                listOf(DozyColors.brandTeal.copy(alpha = .08f), Color.Transparent),
                center = Offset(size.width, size.height * .03f),
                radius = size.width * .84f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(DozyColors.brandCoral.copy(alpha = .04f), Color.Transparent),
                center = Offset(0f, size.height * .18f),
                radius = size.width * .72f,
            ),
        )
    }
}

@Composable
private fun HomeHeader() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DozyLayout.screenGutter, vertical = UiSpacing.x2),
        verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
    ) {
        Text(stringResource(Res.string.good_evening), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(Res.string.home_header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreateStoryBanner(onClick: () -> Unit) {
    val shape = RoundedCornerShape(DozyLayout.bannerRadius)
    Surface(
        shape = shape,
        color = DozyColors.brandCoral,
        contentColor = DozyColors.onBrandCoral,
        modifier = Modifier.fillMaxWidth().shadow(8.dp, shape).clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(UiSpacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                    Text(
                        stringResource(Res.string.home_banner_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        stringResource(Res.string.home_banner_title),
                        style = DozyTextStyles.bannerTitle,
                    )
                    Text(
                        stringResource(Res.string.home_banner_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                        Row(
                            Modifier.padding(horizontal = UiSpacing.x2, vertical = UiSpacing.x1),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1),
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                stringResource(Res.string.create_story),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoriesSection(
    stories: List<Story>,
    onStorySelected: (Story) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        SectionHeading(stringResource(Res.string.home_stories_label), stringResource(Res.string.home_stories_title), onSeeAll)
        stories.forEach { story -> StoryWideCard(story, onStorySelected) }
    }
}

@Composable
private fun CharactersSection(
    characters: List<HomeCharacter>,
    onAddCharacter: () -> Unit,
    onCharacterSelected: (HomeCharacter) -> Unit,
    onSeeAll: () -> Unit,
) {
    val avatarSize = 70.dp
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        SectionHeading(stringResource(Res.string.home_characters_label), stringResource(Res.string.home_characters_title), onSeeAll)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            characters.forEach { character ->
                Column(
                    Modifier.clickable {
                        onCharacterSelected(character)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
                ) {
                    AvatarImage(character.avatarUrl, character.name, Modifier.size(avatarSize).clip(CircleShape))
                    Text(character.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Column(
                Modifier.clickable(onClick = onAddCharacter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
            ) {
                Surface(
                    modifier = Modifier.size(avatarSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = stringResource(Res.string.add_character),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Text(
                    stringResource(Res.string.add),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeSectionStatus(
    state: HomeUiState,
    section: HomeSection,
    onAction: (HomeAction) -> Unit,
) {
    if (section in state.loading) CircularProgressIndicator()
    state.errors[section]?.let {
        Text(it.resolve(), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { onAction(HomeAction.RetrySection(section)) }, enabled = section !in state.refreshing) {
            Text(stringResource(Res.string.place_retry))
        }
    }
}

@Composable
private fun HomeJob(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        HomeSectionStatus(state, HomeSection.GENERATION, onAction)
        state.generation.homeJob?.let { job ->
            val unavailable = job.statusUnknown || job.retryPending
            val failed = job.status == GenerationJob.Status.FAILED && !unavailable
            val ready = job.storyId != null && !unavailable
            val colors = MaterialTheme.colorScheme
            val accent = if (failed) colors.primary else colors.onSecondaryContainer
            val background = if (failed) DozyColors.selectedBackground else colors.secondaryContainer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = background,
                contentColor = colors.onBackground,
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Column(Modifier.padding(DozyLayout.screenGutter), verticalArrangement = Arrangement.spacedBy(DozyLayout.contentGap)) {
                    val label =
                        when {
                            ready -> Res.string.home_job_ready_label
                            failed -> Res.string.home_job_failed_label
                            else -> Res.string.home_job_active_label
                        }
                    val title =
                        when {
                            ready -> Res.string.home_job_ready_title
                            failed -> Res.string.home_job_failed_title
                            unavailable -> Res.string.generation_unavailable
                            else -> Res.string.home_job_active_title
                        }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DozyLayout.labelGap)) {
                        if (!ready && !failed && !unavailable) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = accent, strokeWidth = 2.dp)
                        }
                        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                    Text(stringResource(title), style = DozyTextStyles.bannerTitle)
                    if (!unavailable) {
                        val description =
                            when {
                                ready ->
                                    state.stories
                                        .firstOrNull { it.id == job.storyId?.value }
                                        ?.title
                                        ?.resolve() ?: job.message().resolve()
                                failed -> job.message().resolve()
                                else -> stringResource(Res.string.home_job_active_body)
                            }
                        Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    val action =
                        when {
                            ready -> Res.string.generation_read
                            failed -> Res.string.place_retry
                            else -> Res.string.generation_progress
                        }
                    DozyActionButton(
                        onClick = {
                            when {
                                ready -> onAction(HomeAction.Generation(GenerationAction.Open(job.id)))
                                failed -> onAction(HomeAction.Generation(GenerationAction.Retry(job.id)))
                                else -> onAction(HomeAction.ProgressClicked(job.id.value))
                            }
                        },
                        enabled = !state.generation.busy && !state.navigating && (!failed || HomeSection.GENERATION !in state.refreshing),
                    ) {
                        Text(stringResource(action))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePlaces(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(stringResource(Res.string.home_places_label), stringResource(Res.string.places_heading), {
            onAction(HomeAction.SeeAllPlacesClicked)
        })
        HomeSectionStatus(state, HomeSection.PLACES, onAction)
        state.places.forEach { place ->
            PlaceCard(place, enabled = !state.navigating, onSelect = { onAction(HomeAction.PlaceSelected(place.id)) })
        }
        if (HomeSection.PLACES !in state.loading && HomeSection.PLACES !in state.errors && state.places.isEmpty()) {
            Text(stringResource(Res.string.place_empty))
        }
    }
}
