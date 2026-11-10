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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dozy.mobile.core.presentation.components.AmbientGlow
import com.dozy.mobile.core.presentation.components.Eyebrow
import com.dozy.mobile.core.presentation.components.SectionHeading
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.feature.story.presentation.components.StoryCard
import com.dozy.mobile.feature.story.presentation.components.StoryWideCard
import com.kjursa.composeuikit.mvi.MviScreen
import com.kjursa.composeuikit.theme.UiSpacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun StoriesTabScreen() {
    val storiesViewModel: StoriesViewModel = koinViewModel()
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
