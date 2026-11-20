package com.dozy.mobile.feature.home.presentation

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dozy.mobile.core.presentation.components.AmbientGlow
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.core.presentation.components.SectionHeading
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.feature.place.presentation.list.PlaceCard
import com.dozy.mobile.feature.story.presentation.components.StoryWideCard
import com.dozy.mobile.feature.story.presentation.generation.GenerationCard
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
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader()
            if (state.firstVisit) {
                FirstVisitHome(onCreateCharacter = { onAction(HomeAction.AddCharacterClicked) })
                Spacer(Modifier.height(mainBottomMenuScrollSpace()))
                return@Column
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = DozyLayout.screenGutter),
                verticalArrangement = Arrangement.spacedBy(DozyLayout.sectionGap),
            ) {
                HomeJob(state, onAction)
                CreateStoryBanner { onAction(HomeAction.CreateStoryClicked) }
                Column {
                    HomeSectionStatus(state, HomeSection.STORIES, onAction)
                    StoriesSection(state.stories, onStorySelected = {
                        onAction(HomeAction.StorySelected(it))
                    }, onSeeAll = { onAction(HomeAction.SeeAllStoriesClicked) })
                    if (HomeSection.STORIES !in state.loading &&
                        HomeSection.STORIES !in state.refreshing &&
                        HomeSection.STORIES !in state.errors &&
                        state.stories.isEmpty() &&
                        state.characters.isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.home_first_story))
                    }
                }
                Column {
                    HomeSectionStatus(state, HomeSection.CHARACTERS, onAction)
                    CharactersSection(
                        state.characters,
                        onAddCharacter = {
                            onAction(HomeAction.AddCharacterClicked)
                        },
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
private fun HomeHeader() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DozyLayout.screenGutter, vertical = UiSpacing.x2),
        verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
    ) {
        Text(
            stringResource(Res.string.tuesday_14_october),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(stringResource(Res.string.good_evening), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(Res.string.home_header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FirstVisitHome(onCreateCharacter: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DozyLayout.screenGutter, vertical = UiSpacing.x4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiSpacing.x2),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(145.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Text(
            stringResource(Res.string.home_first_visit_eyebrow),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            stringResource(Res.string.home_first_visit_title),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(Res.string.home_first_visit),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DozyActionButton(onClick = onCreateCharacter) {
            Text(stringResource(Res.string.home_first_character))
        }
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
        SectionHeading(stringResource(Res.string.library), stringResource(Res.string.your_stories), onSeeAll)
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
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        SectionHeading(stringResource(Res.string.cast), stringResource(Res.string.your_characters), onSeeAll)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            characters.forEach { character ->
                Column(
                    Modifier.clickable {
                        onCharacterSelected(character)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
                ) {
                    AvatarImage(character.avatarUrl, character.name, Modifier.size(80.dp))
                    Text(character.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Column(
                Modifier.clickable(onClick = onAddCharacter),
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
    if (section in state.loading || section in state.refreshing) CircularProgressIndicator()
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
            GenerationCard(job, state.generation.busy) {
                onAction(HomeAction.Generation(it))
            }
            TextButton(onClick = { onAction(HomeAction.ProgressClicked(job.id.value)) }) {
                Text(stringResource(Res.string.generation_progress))
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
        SectionHeading(stringResource(Res.string.place_setting), stringResource(Res.string.places_heading), {
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
