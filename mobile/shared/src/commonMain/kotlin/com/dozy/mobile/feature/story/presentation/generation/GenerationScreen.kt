package com.dozy.mobile.feature.story.presentation.generation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.generation_background
import com.dozy.mobile.resources.generation_enable_notifications
import com.dozy.mobile.resources.generation_heading
import com.dozy.mobile.resources.generation_read
import com.dozy.mobile.resources.generation_refresh
import com.dozy.mobile.resources.generation_unavailable
import com.dozy.mobile.resources.place_retry
import com.dozy.mobile.resources.story_back_home
import com.dozy.mobile.ui.common.DozyActionButton
import com.dozy.mobile.ui.common.DozyLayout
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun GenerationScreen(
    jobId: String,
    onHome: () -> Unit,
    requestNotifications: (() -> Unit)? = null,
    backgroundCompletionSupported: Boolean = false,
) {
    val vm: GenerationViewModel = koinViewModel()
    MviScreen(vm) { state, dispatch ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(DozyLayout.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DozyLayout.contentGap),
        ) {
            Text(
                stringResource(Res.string.generation_heading),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            if (state.loading) CircularProgressIndicator()
            val job = state.jobs.find { it.id.value == jobId }
            if (!state.loading && job == null) Text(stringResource(Res.string.generation_unavailable))
            job?.let { GenerationCard(it, state.busy, dispatch) }
            state.error?.let { Text(it.resolve()) }
            if (backgroundCompletionSupported) {
                Text(
                    stringResource(Res.string.generation_background),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            if (requestNotifications != null) {
                TextButton(onClick = { requestNotifications() }) {
                    Text(stringResource(Res.string.generation_enable_notifications))
                }
            }
            DozyActionButton(onClick = onHome) { Text(stringResource(Res.string.story_back_home)) }
        }
    }
}

@Composable
internal fun GenerationCard(
    job: GenerationJob,
    busy: Boolean,
    dispatch: (GenerationAction) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(job.message().resolve(), style = MaterialTheme.typography.titleMedium)
            if (job.status !in setOf(GenerationJob.Status.READY, GenerationJob.Status.FAILED) &&
                !job.statusUnknown
            ) {
                LinearProgressIndicator()
            }
            if (job.storyId != null) {
                Button(
                    onClick = { dispatch(GenerationAction.Open(job.id)) },
                    enabled = !busy,
                ) { Text(stringResource(Res.string.generation_read)) }
            } else {
                if (job.status == GenerationJob.Status.FAILED && !job.statusUnknown && !job.retryPending) {
                    Button(
                        onClick = { dispatch(GenerationAction.Retry(job.id)) },
                        enabled = !busy,
                    ) { Text(stringResource(Res.string.place_retry)) }
                }
                TextButton(
                    onClick = { dispatch(GenerationAction.Refresh(job.id)) },
                    enabled = !busy,
                ) { Text(stringResource(Res.string.generation_refresh)) }
            }
        }
    }
}
