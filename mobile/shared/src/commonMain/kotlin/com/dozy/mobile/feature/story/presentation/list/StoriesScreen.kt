package com.dozy.mobile.feature.story.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dozy.mobile.core.presentation.components.CollectionEmptyState
import com.dozy.mobile.core.presentation.components.CollectionScreen
import com.dozy.mobile.feature.story.presentation.components.StoryWideCard
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.my_stories
import com.dozy.mobile.resources.stories_collection_empty_action
import com.dozy.mobile.resources.stories_collection_empty_description
import com.dozy.mobile.resources.stories_collection_empty_title
import com.dozy.mobile.resources.stories_collection_eyebrow
import com.dozy.mobile.resources.stories_collection_intro
import com.dozy.mobile.resources.stories_collection_title
import com.dozy.mobile.resources.stories_create
import com.dozy.mobile.resources.stories_offline
import com.dozy.mobile.resources.stories_refresh
import com.dozy.mobile.ui.theme.BottomTabPalette
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun StoriesTabScreen(onBack: () -> Unit) {
    val storiesViewModel: StoriesViewModel = koinViewModel()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { storiesViewModel.navigationHandled() }
    MviScreen(
        viewModel = storiesViewModel,
    ) { state, dispatch ->
        StoriesTabContent(state, dispatch, onBack)
    }
}

@Composable
internal fun StoriesTabContent(
    state: StoriesUiState,
    dispatch: (StoriesAction) -> Unit,
    onBack: () -> Unit = {},
) {
    val create = { dispatch(StoriesAction.Create) }
    CollectionScreen(
        palette = BottomTabPalette.STORIES,
        toolbarTitle = stringResource(Res.string.my_stories),
        eyebrow = stringResource(Res.string.stories_collection_eyebrow),
        title = stringResource(Res.string.stories_collection_title),
        intro = stringResource(Res.string.stories_collection_intro),
        addDescription = stringResource(Res.string.stories_create),
        onBack = onBack,
        onRefresh = { dispatch(StoriesAction.Refresh) },
        refreshing = state.refreshing,
        onAdd = create,
        addEnabled = !state.navigating,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.loading) CircularProgressIndicator()
            state.error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
            if (state.offline) Text(stringResource(Res.string.stories_offline))
            if (state.error != null || state.offline) {
                TextButton(enabled = !state.refreshing, onClick = { dispatch(StoriesAction.Refresh) }) {
                    Text(stringResource(Res.string.stories_refresh))
                }
            }
            if (!state.loading && !state.refreshing && state.error == null && state.stories.isEmpty()) {
                CollectionEmptyState(
                    Icons.Default.AutoStories,
                    stringResource(Res.string.stories_collection_empty_title),
                    stringResource(Res.string.stories_collection_empty_description),
                    stringResource(Res.string.stories_collection_empty_action),
                    create,
                    enabled = !state.navigating,
                )
            }
            state.stories.forEach { story ->
                StoryWideCard(story) { dispatch(StoriesAction.StorySelected(it)) }
            }
        }
    }
}
