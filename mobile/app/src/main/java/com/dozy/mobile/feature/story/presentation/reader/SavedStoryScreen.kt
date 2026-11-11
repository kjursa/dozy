package com.dozy.mobile.feature.story.presentation.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dozy.mobile.R
import org.koin.compose.viewmodel.koinViewModel

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
