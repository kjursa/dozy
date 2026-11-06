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
import androidx.core.view.WindowCompat
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
import com.dozy.mobile.ui.theme.DozySpacing
import com.dozy.mobile.ui.theme.DozyTheme
import com.dozy.mobile.ui.theme.DozyColorScheme

private data class Story(val title: String, val minutes: String, val image: Int)

private val stories = listOf(
    Story("Oliver and the Secret Pirate Island", "6 min read", R.drawable.story_pirates),
    Story("The Mystery of the Missing Moon", "10 min read", R.drawable.story_moon),
)

private data class Character(val name: String, val image: Int)

private val characters = listOf(
    Character("Oliver", R.drawable.character_oliver),
    Character("Luna", R.drawable.character_luna),
    Character("Captain Finn", R.drawable.character_finn),
)

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
internal fun HomeScreen(onCreateStory: () -> Unit, onAddCharacter: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeHeader()
            Column(
                Modifier.fillMaxWidth().padding(horizontal = DozySpacing.x3),
                verticalArrangement = Arrangement.spacedBy(DozySpacing.x4),
            ) {
                CreateStoryBanner(onClick = onCreateStory)
                StoriesSection()
                CharactersSection(onAddCharacter)
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
private fun AmbientGlow(modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(Modifier.padding(top = 5.dp).size(190.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .07f), CircleShape))
        Box(Modifier.align(Alignment.TopEnd).offset(x = 90.dp, y = 145.dp).size(220.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = .09f), CircleShape))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-70).dp, y = (-110).dp).size(180.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha = .14f), CircleShape))
    }
}

@Composable
private fun HomeHeader() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = DozySpacing.x3, vertical = DozySpacing.x2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
            Eyebrow("TUESDAY, 14 OCTOBER")
            Text("Good evening!", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Ready for a new adventure?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            modifier = Modifier.size(48.dp).shadow(8.dp, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
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
        Row(Modifier.padding(DozySpacing.x3), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
                Text("Create a New Story", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                Text("Tell us who the heroes are and we'll dream up tonight's tale.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .92f))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.padding(horizontal = DozySpacing.x2, vertical = DozySpacing.x1), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Create Story", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Image(painterResource(R.drawable.story_moon), contentDescription = "Open magical book", modifier = Modifier.size(92.dp), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun StoriesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
        SectionHeading("LIBRARY", "Your Stories")
        Row(horizontalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
            stories.forEach { story -> StoryCard(story, Modifier.weight(1f)) }
        }
        StoryWideCard(Story("A Journey Through the Enchanted Forest", "8 min read", R.drawable.story_forest))
    }
}

@Composable
private fun SectionHeading(kicker: String, title: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column {
            Eyebrow(kicker)
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Text("See all", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
private fun StoryCard(story: Story, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(8.dp, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Column(Modifier.padding(DozySpacing.x2), verticalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
            Image(painterResource(story.image), contentDescription = "${story.title} cover", modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
            Column(Modifier.padding(horizontal = DozySpacing.x1), verticalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
                Text(story.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                ReadTime(story.minutes)
            }
        }
    }
}

@Composable
private fun StoryWideCard(story: Story) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Row(Modifier.padding(DozySpacing.x2), horizontalArrangement = Arrangement.spacedBy(DozySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(story.image), contentDescription = "${story.title} cover", modifier = Modifier.width(96.dp).aspectRatio(3f / 4f).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f).padding(end = DozySpacing.x1), verticalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
                Text(story.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                ReadTime(story.minutes)
            }
        }
    }
}

@Composable
private fun ReadTime(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StoriesTabScreen() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = DozySpacing.x3),
                verticalArrangement = Arrangement.spacedBy(DozySpacing.x4),
            ) {
                Column(Modifier.padding(top = DozySpacing.x2), verticalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
                    Eyebrow("YOUR LIBRARY")
                    Text("My Stories", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text("Pick up where your last adventure left off.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StoriesSection()
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
internal fun CharactersTabScreen(onAddCharacter: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientGlow(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = DozySpacing.x3),
                verticalArrangement = Arrangement.spacedBy(DozySpacing.x4),
            ) {
                Column(Modifier.padding(top = DozySpacing.x2), verticalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
                    Eyebrow("YOUR CAST")
                    Text("Characters", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text("Meet the heroes of your bedtime stories.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                characters.chunked(2).forEach { rowCharacters ->
                    Row(horizontalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
                        rowCharacters.forEach { character ->
                            CharacterLibraryCard(character, Modifier.weight(1f))
                        }
                        if (rowCharacters.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                CharacterLibraryCard(onAddCharacter = onAddCharacter)
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
private fun CharacterLibraryCard(character: Character, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(6.dp, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Column(Modifier.padding(DozySpacing.x2), verticalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
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
        Row(Modifier.padding(DozySpacing.x3), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
            Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
            Column {
                Text("Add a new character", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Create another hero for your stories.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CharactersSection(onAddCharacter: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
        SectionHeading("CAST", "Your Characters")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DozySpacing.x2)) {
            characters.forEach { character ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
                    Image(painterResource(character.image), contentDescription = character.name, modifier = Modifier.size(80.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White).padding(3.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Text(character.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Column(Modifier.clickable(onClick = onAddCharacter), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DozySpacing.x1)) {
                Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AddCircle, contentDescription = "Add character", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp)) }
                }
                Text("Add", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

