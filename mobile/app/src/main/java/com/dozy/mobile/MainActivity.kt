package com.dozy.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dozy.mobile.ui.theme.DozyColorScheme
import com.dozy.mobile.ui.theme.DozyTheme
import com.kjursa.composeuikit.mvi.MviScreen
import com.kjursa.composeuikit.theme.UiSpacing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = DozyColorScheme.background.toArgb()
        window.navigationBarColor = DozyColorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            DozyTheme {
                DozyApp()
            }
        }
    }
}

@Composable
internal fun HomeScreen(
    onCreateStory: () -> Unit,
    onAddCharacter: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel()
    MviScreen(
        viewModel = homeViewModel,
        onEffect = { effect ->
            when (effect) {
                HomeEffect.CreateStoryRequested -> onCreateStory()
                HomeEffect.AddCharacterRequested -> onAddCharacter()
                is HomeEffect.OpenStory, HomeEffect.SeeAllStoriesRequested,
                is HomeEffect.OpenCharacter, HomeEffect.OpenNotifications -> Unit
            }
        },
    ) { state, dispatch ->
        HomeContent(state = state, onAction = dispatch)
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
            HomeHeader(state, onNotificationsClick = { onAction(HomeAction.NotificationsClicked) })
            Column(
                Modifier.fillMaxWidth().padding(horizontal = UiSpacing.x3),
                verticalArrangement = Arrangement.spacedBy(UiSpacing.x4),
            ) {
                CreateStoryBanner { onAction(HomeAction.CreateStoryClicked) }
                StoriesSection(state.stories, onStorySelected = { onAction(HomeAction.StorySelected(it)) }, onSeeAll = { onAction(HomeAction.SeeAllStoriesClicked) })
                CharactersSection(state.characters, onAddCharacter = { onAction(HomeAction.AddCharacterClicked) }, onCharacterSelected = { onAction(HomeAction.CharacterSelected(it)) })
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
private fun AmbientGlow(modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(Modifier.padding(top = 5.dp).size(190.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .07f), CircleShape))
        Box(
            Modifier
                .align(
                    Alignment.TopEnd,
                ).offset(x = 90.dp, y = 145.dp)
                .size(220.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = .09f), CircleShape),
        )
        Box(
            Modifier
                .align(
                    Alignment.BottomStart,
                ).offset(
                    x = (-70).dp,
                    y = (-110).dp,
                ).size(180.dp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = .14f), CircleShape),
        )
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, onNotificationsClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = UiSpacing.x3, vertical = UiSpacing.x2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            Eyebrow(state.dateLabel)
            Text(state.greeting, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                state.prompt,
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
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun Eyebrow(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Create a New Story", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    "Tell us who the heroes are and we'll dream up tonight's tale.",
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
                        Text("Create Story", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Image(
                painterResource(R.drawable.story_moon),
                contentDescription = "Open magical book",
                modifier = Modifier.size(92.dp),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun StoriesSection(stories: List<Story>, onStorySelected: (Story) -> Unit, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        SectionHeading("LIBRARY", "Your Stories", onSeeAll)
        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            stories.take(2).forEach { story -> StoryCard(story, Modifier.weight(1f), onStorySelected) }
        }
        stories.getOrNull(2)?.let { StoryWideCard(it, onStorySelected) }
    }
}

@Composable
private fun SectionHeading(
    kicker: String,
    title: String,
    onSeeAll: () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column {
            Eyebrow(kicker)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            "See all",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 3.dp).clickable(onClick = onSeeAll),
        )
    }
}

@Composable
private fun StoryCard(
    story: Story,
    modifier: Modifier = Modifier,
    onClick: (Story) -> Unit = {},
) {
    Surface(
        modifier = modifier.shadow(8.dp, MaterialTheme.shapes.large).clickable { onClick(story) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Column(Modifier.padding(UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            Image(
                painterResource(story.image),
                contentDescription = "${story.title} cover",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            3f / 4f,
                        ).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.padding(horizontal = UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(
                    story.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ReadTime(story.minutes)
            }
        }
    }
}

@Composable
private fun StoryWideCard(
    story: Story,
    onClick: (Story) -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, MaterialTheme.shapes.large).clickable { onClick(story) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Row(
            Modifier.padding(UiSpacing.x2),
            horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(story.image),
                contentDescription = "${story.title} cover",
                modifier =
                    Modifier
                        .width(96.dp)
                        .aspectRatio(
                            3f / 4f,
                        ).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(end = UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(story.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                ReadTime(story.minutes)
            }
        }
    }
}

@Composable
private fun ReadTime(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StoriesTabScreen() {
    val storiesViewModel: StoriesViewModel = viewModel()
    MviScreen(
        viewModel = storiesViewModel,
        onEffect = { effect ->
            when (effect) {
                is StoriesEffect.OpenStory -> Unit // A story detail destination does not exist yet.
            }
        },
    ) { state, dispatch ->
        StoriesTabContent(state) { story -> dispatch(StoriesAction.StorySelected(story)) }
    }
}

@Composable
internal fun StoriesTabContent(
    state: StoriesUiState,
    onStorySelected: (Story) -> Unit,
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
                    Text(state.subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                    SectionHeading(state.collectionEyebrow, state.collectionTitle)
                    Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                        state.stories.take(2).forEach { story -> StoryCard(story, Modifier.weight(1f), onStorySelected) }
                    }
                    state.stories.getOrNull(2)?.let { StoryWideCard(it, onStorySelected) }
                }
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
internal fun CharactersTabScreen(onAddCharacter: () -> Unit) {
    val state = HomeUiState()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = UiSpacing.x3),
            verticalArrangement = Arrangement.spacedBy(UiSpacing.x4),
        ) {
            Column(Modifier.padding(top = UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Eyebrow("CAST")
                Text("Your Characters", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text("Choose a friend for your next story.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CharactersSection(state.characters, onAddCharacter = onAddCharacter, onCharacterSelected = {})
        }
    }
}

@Composable
private fun CharactersSection(characters: List<HomeCharacter>, onAddCharacter: () -> Unit, onCharacterSelected: (HomeCharacter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        SectionHeading("CAST", "Your Characters")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            characters.forEach { character ->
                Column(Modifier.clickable { onCharacterSelected(character) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                    Image(
                        painterResource(character.image),
                        contentDescription = character.name,
                        modifier =
                            Modifier
                                .size(
                                    80.dp,
                                ).shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(3.dp)
                                .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
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
                            contentDescription = "Add character",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Text("Add", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
