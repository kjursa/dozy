package com.dozy.mobile.feature.story.presentation.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dozy.mobile.R
import com.dozy.mobile.core.presentation.components.AvatarImage
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SavedStoryScreen(
    id: String,
    onBack: () -> Unit,
) {
    val vm: SavedStoryViewModel = koinViewModel()
    val stories by vm.stories.collectAsStateWithLifecycle()
    val story = stories?.find { it.id.value == id }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.generation_back)) }
        if (stories == null) {
            CircularProgressIndicator()
        } else if (story == null) {
            Text(stringResource(R.string.generation_story_unavailable))
        } else {
            Text(story.title, style = MaterialTheme.typography.headlineLarge)
            AvatarImage(story.coverUrl, story.title, Modifier.fillMaxWidth().height(240.dp))
            Text(story.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun SavedStories(onRead: (String) -> Unit) {
    val vm: SavedStoryViewModel = koinViewModel()
    val stories by vm.stories.collectAsStateWithLifecycle()
    Column(Modifier.padding(24.dp)) {
        Text(stringResource(R.string.generation_saved), style = MaterialTheme.typography.titleLarge)
        if (stories == null) {
            CircularProgressIndicator()
        } else if (stories.orEmpty().isEmpty()) {
            Text(stringResource(R.string.generation_empty))
        }
        stories.orEmpty().forEach { story ->
            TextButton(onClick = { onRead(story.id.value) }) { Text(story.title) }
        }
    }
}
