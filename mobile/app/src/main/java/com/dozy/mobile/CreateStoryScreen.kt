package com.dozy.mobile

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kjursa.composeuikit.theme.UiSpacing

private data class StoryThemeOption(val title: String, val image: Int)
private data class StoryHero(val name: String, val image: Int)

private val storyThemes = listOf(
    StoryThemeOption("Pirates", R.drawable.story_pirates),
    StoryThemeOption("Detectives", R.drawable.story_detectives),
    StoryThemeOption("Enchanted Forest", R.drawable.story_forest),
    StoryThemeOption("Space", R.drawable.story_space),
)

private val storyHeroes = listOf(
    StoryHero("Oliver", R.drawable.character_oliver),
    StoryHero("Luna", R.drawable.character_luna),
    StoryHero("Captain Finn", R.drawable.character_finn),
)

@Composable
fun CreateStoryScreen(
    onBack: () -> Unit,
    onNewHero: () -> Unit,
    onGenerate: () -> Unit,
) {
    var selectedTheme by rememberSaveable { mutableIntStateOf(0) }
    val selectedHeroes = remember { mutableStateListOf("Oliver", "Captain Finn") }
    var selectedLength by rememberSaveable { mutableIntStateOf(0) }
    var specialRequest by rememberSaveable { mutableStateOf("") }
    var ending by rememberSaveable { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StoryScreenGlows()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            StoryHeader(onBack)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = UiSpacing.x3),
                verticalArrangement = Arrangement.spacedBy(UiSpacing.x4),
            ) {
                StoryIntro()
                ThemePicker(selectedTheme, onSelect = { selectedTheme = it })
                HeroPicker(selectedHeroes, onToggle = { name ->
                    if (name in selectedHeroes) selectedHeroes.remove(name) else selectedHeroes.add(name)
                }, onNewHero = onNewHero)
                LengthPicker(selectedLength, onSelect = { selectedLength = it })
                StoryPrompts(
                    specialRequest = specialRequest,
                    onSpecialRequestChange = { specialRequest = it },
                    ending = ending,
                    onEndingChange = { ending = it },
                )
                Spacer(Modifier.height(mainBottomMenuScrollSpace()))
            }
        }
        Button(
            onClick = onGenerate,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding().padding(horizontal = UiSpacing.x3, vertical = UiSpacing.x3)
                .height(56.dp).shadow(16.dp, CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(UiSpacing.x1))
            Text("Generate My Story", style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun StoryScreenGlows() {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.TopStart).offset(x = (-80).dp, y = (-64).dp).size(210.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha = .17f), CircleShape))
        Box(Modifier.align(Alignment.TopEnd).offset(x = 90.dp, y = 300.dp).size(190.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = .09f), CircleShape))
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.secondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                val isActive = index < 3
                Box(
                    Modifier.size(width = if (index == 2) 24.dp else 8.dp, height = 8.dp)
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
        Text("STEP 1 OF 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Where shall we go tonight?", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Text("Pick one adventure theme.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemePicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        storyThemes.chunked(2).forEachIndexed { rowIndex, rowThemes ->
            Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                rowThemes.forEachIndexed { columnIndex, item ->
                    val index = rowIndex * 2 + columnIndex
                    ThemeCard(item, selected = index == selected, modifier = Modifier.weight(1f), onClick = { onSelect(index) })
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(option: StoryThemeOption, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.shadow(6.dp, MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
            Box {
                Image(
                    painterResource(option.image),
                    contentDescription = option.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
                if (selected) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(UiSpacing.x1),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(5.dp).size(16.dp))
                    }
                }
            }
            Text(option.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HeroPicker(selected: List<String>, onToggle: (String) -> Unit, onNewHero: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Column {
            Text("HEROES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Who will join the adventure?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            storyHeroes.forEach { hero ->
                val isSelected = hero.name in selected
                Column(
                    modifier = Modifier.clickable { onToggle(hero.name) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiSpacing.x1),
                ) {
                    Box {
                        Image(
                            painterResource(hero.image),
                            contentDescription = hero.name,
                            modifier = Modifier.size(80.dp).shadow(5.dp, CircleShape).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        if (isSelected) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                    Text(hero.name, style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                    }
                }
                Text("New hero", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LengthPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Column {
            Text("LENGTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("How long should the story be?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            LengthCard("Short", "A little bedtime adventure", "3–5 min", Icons.Default.Bedtime, selected == 0, Modifier.weight(1f)) { onSelect(0) }
            LengthCard("Long", "One more chapter of magic", "8–12 min", Icons.Default.AutoAwesome, selected == 1, Modifier.weight(1f)) { onSelect(1) }
        }
    }
}

@Composable
private fun LengthCard(title: String, subtitle: String, length: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Surface(
        modifier = modifier.shadow(6.dp, MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = tint.copy(alpha = .16f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp)) }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(length, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StoryPrompts(specialRequest: String, onSpecialRequestChange: (String) -> Unit, ending: String, onEndingChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
        Column {
            Text("OPTIONAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Anything special you'd like?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
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
                    placeholder = "Maybe the heroes discover a hidden treasure, solve a mystery, or make a new friend...",
                    minimumHeight = 96.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                    Text("How should it end?  (optional)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    StoryInput(
                        value = ending,
                        onValueChange = onEndingChange,
                        placeholder = "Oliver finds the lost treasure and shares it with his friends.",
                        minimumHeight = 64.dp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Text("Leave empty and we'll create a surprise ending.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryInput(value: String, onValueChange: (String) -> Unit, placeholder: String, minimumHeight: androidx.compose.ui.unit.Dp) {
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
