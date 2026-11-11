package com.dozy.mobile.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dozy.mobile.R
import com.dozy.mobile.core.presentation.components.AmbientGlow
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.core.presentation.components.Eyebrow
import com.dozy.mobile.core.presentation.components.SectionHeading
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.presentation.list.PlaceCard
import com.dozy.mobile.feature.place.presentation.list.PlacesAction
import com.dozy.mobile.feature.place.presentation.list.PlacesEffect
import com.dozy.mobile.feature.place.presentation.list.PlacesViewModel
import com.dozy.mobile.feature.story.presentation.components.StoryCard
import com.dozy.mobile.feature.story.presentation.components.StoryWideCard
import com.dozy.mobile.feature.story.presentation.list.Story
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.compose.theme.UiSpacing
import com.kjursa.foundation.presentation.text.resolve
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    onCreateStory: () -> Unit,
    onAddCharacter: () -> Unit,
    onCharacter: (String) -> Unit,
    onPlaces: () -> Unit,
    onPlaceSelected: (Place.Id?) -> Unit,
    onGeneration: (String) -> Unit,
    onRead: (String) -> Unit,
) {
    val homeViewModel: HomeViewModel = koinViewModel()
    MviScreen(
        viewModel = homeViewModel,
        onEffect = { effect ->
            when (effect) {
                HomeEffect.CreateStoryRequested -> onCreateStory()
                HomeEffect.AddCharacterRequested -> onAddCharacter()
                is HomeEffect.OpenCharacter -> onCharacter(effect.character.id)
                is HomeEffect.OpenStory, HomeEffect.SeeAllStoriesRequested,
                HomeEffect.OpenNotifications,
                -> Unit
            }
        },
    ) { state, dispatch ->
        Column {
            if (state.characterLoadError) Text(stringResource(R.string.could_not_load_characters), color = MaterialTheme.colorScheme.error)
            HomeContent(
                state = state,
                onAction = dispatch,
                onPlaces = onPlaces,
                onPlaceSelected = onPlaceSelected,
                onGeneration = onGeneration,
                onRead = onRead,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onPlaces: () -> Unit,
    onPlaceSelected: (Place.Id?) -> Unit,
    onGeneration: (String) -> Unit,
    onRead: (String) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader(state, onNotificationsClick = { onAction(HomeAction.NotificationsClicked) })
            Column(
                Modifier.fillMaxWidth().padding(horizontal = UiSpacing.x3),
                verticalArrangement = Arrangement.spacedBy(UiSpacing.x4),
            ) {
                com.dozy.mobile.feature.story.presentation.generation
                    .HomeGeneration(onGeneration, onRead)
                com.dozy.mobile.feature.story.presentation.reader
                    .SavedStories(onRead)
                CreateStoryBanner { onAction(HomeAction.CreateStoryClicked) }
                StoriesSection(state.stories, onStorySelected = {
                    onAction(HomeAction.StorySelected(it))
                }, onSeeAll = { onAction(HomeAction.SeeAllStoriesClicked) })
                CharactersSection(state.characters, onAddCharacter = {
                    onAction(HomeAction.AddCharacterClicked)
                }, onCharacterSelected = { onAction(HomeAction.CharacterSelected(it)) })
            }
            HomePlaces(onPlaces, onPlaceSelected)
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onNotificationsClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = UiSpacing.x3, vertical = UiSpacing.x2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            Eyebrow(state.dateLabel.resolve())
            Text(state.greeting.resolve(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                state.prompt.resolve(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.size(48.dp).shadow(8.dp, CircleShape).clickable(onClick = onNotificationsClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.notifications),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun CreateStoryBanner(onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().shadow(8.dp, MaterialTheme.shapes.large).clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(UiSpacing.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                Text(
                    stringResource(R.string.create_a_new_story),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    stringResource(R.string.tell_us_who_the_heroes_are_and_we_ll),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .92f),
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
                            stringResource(R.string.create_story),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Image(
                painterResource(R.drawable.story_moon),
                contentDescription = stringResource(R.string.open_magical_book),
                modifier = Modifier.size(92.dp),
                contentScale = ContentScale.Crop,
            )
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
        SectionHeading(stringResource(R.string.library), stringResource(R.string.your_stories), onSeeAll)
        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            stories.take(2).forEach { story -> StoryCard(story, Modifier.weight(1f), onStorySelected) }
        }
        stories.getOrNull(2)?.let { StoryWideCard(it, onStorySelected) }
    }
}

@Composable
private fun CharactersSection(
    characters: List<HomeCharacter>,
    onAddCharacter: () -> Unit,
    onCharacterSelected: (HomeCharacter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        SectionHeading(stringResource(R.string.cast), stringResource(R.string.your_characters))
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
                            contentDescription = stringResource(R.string.add_character),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.add),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomePlaces(
    onPlaces: () -> Unit,
    onPlaceSelected: (Place.Id?) -> Unit,
) {
    val viewModel: PlacesViewModel = koinViewModel()
    MviScreen(viewModel, onEffect = { effect ->
        if (effect is PlacesEffect.Selected) onPlaceSelected(effect.id)
        viewModel.navigationHandled()
    }) { state, dispatch ->
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading(stringResource(R.string.place_setting), stringResource(R.string.places_heading), onPlaces)
            state.places.take(3).forEach { place ->
                PlaceCard(place, enabled = !state.navigating, onSelect = {
                    dispatch(PlacesAction.Select(place.id))
                })
            }
            if (state.loading) CircularProgressIndicator()
            state.error?.let {
                Text(it.resolve())
                TextButton(onClick = { dispatch(PlacesAction.Retry) }) { Text(stringResource(R.string.place_retry)) }
            }
        }
    }
}
