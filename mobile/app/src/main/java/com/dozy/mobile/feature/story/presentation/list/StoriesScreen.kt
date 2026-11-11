package com.dozy.mobile.feature.story.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dozy.mobile.R
import com.dozy.mobile.core.presentation.components.AmbientGlow
import com.dozy.mobile.core.presentation.components.Eyebrow
import com.dozy.mobile.core.presentation.components.SectionHeading
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.feature.story.presentation.components.StoryCard
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.compose.theme.UiSpacing
import com.kjursa.foundation.presentation.text.resolve
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun StoriesTabScreen(
    onRead: (String) -> Unit,
    onCreateStory: () -> Unit,
) {
    val storiesViewModel: StoriesViewModel = koinViewModel()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { storiesViewModel.navigationHandled() }
    MviScreen(
        viewModel = storiesViewModel,
        onEffect = { effect ->
            when (effect) {
                is StoriesEffect.OpenStory -> onRead(effect.story.id)
                StoriesEffect.CreateStory -> onCreateStory()
            }
        },
    ) { state, dispatch ->
        StoriesTabContent(state, dispatch)
    }
}

@Composable
internal fun StoriesTabContent(
    state: StoriesUiState,
    dispatch: (StoriesAction) -> Unit,
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
                    Eyebrow(state.eyebrow.resolve())
                    Text(
                        state.title.resolve(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        state.subtitle.resolve(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                    SectionHeading(state.collectionEyebrow.resolve(), state.collectionTitle.resolve())
                    Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                        Button(enabled = !state.navigating, onClick = { dispatch(StoriesAction.Create) }) {
                            Text(stringResource(R.string.stories_create))
                        }
                        TextButton(enabled = !state.refreshing, onClick = { dispatch(StoriesAction.Refresh) }) {
                            Text(stringResource(R.string.stories_refresh))
                        }
                    }
                    if (state.loading || state.refreshing) CircularProgressIndicator()
                    state.error?.let { Text(it.resolve()) }
                    if (state.offline) Text(stringResource(R.string.stories_offline))
                    if (!state.loading && !state.refreshing && state.error == null && state.stories.isEmpty()) {
                        Text(stringResource(R.string.stories_empty))
                    }
                    state.stories.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
                            pair.forEach { story ->
                                StoryCard(story, Modifier.weight(1f)) { dispatch(StoriesAction.StorySelected(it)) }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}
