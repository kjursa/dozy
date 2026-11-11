package com.dozy.mobile.feature.story.presentation.generation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dozy.mobile.R
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.resolve
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun GenerationScreen(
    jobId: String,
    onHome: () -> Unit,
    onRead: (String) -> Unit,
) {
    val vm: GenerationViewModel = koinViewModel()
    val scheduler: com.dozy.mobile.feature.story.data.background.GenerationWorkScheduler = org.koin.compose.koinInject()
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) scheduler.checkNow() }
    MviScreen(vm, onEffect = { if (it is GenerationEffect.Read) onRead(it.id.value) }) { state, dispatch ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.generation_heading), style = MaterialTheme.typography.headlineLarge)
            if (state.loading) CircularProgressIndicator()
            val job = state.jobs.find { it.id.value == jobId }
            if (!state.loading && job == null) Text(stringResource(R.string.generation_unavailable))
            job?.let { GenerationCard(it, state.busy, dispatch) }
            state.error?.let { Text(it.resolve()) }
            Text(stringResource(R.string.generation_background))
            if (Build.VERSION.SDK_INT >= 33) {
                TextButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.generation_enable_notifications))
                }
            }
            Button(onClick = onHome) { Text(stringResource(R.string.story_back_home)) }
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
                ) { Text(stringResource(R.string.generation_read)) }
            } else {
                if (job.status == GenerationJob.Status.FAILED && !job.statusUnknown) {
                    Button(
                        onClick = { dispatch(GenerationAction.Retry(job.id)) },
                        enabled = !busy,
                    ) { Text(stringResource(R.string.place_retry)) }
                }
                TextButton(
                    onClick = { dispatch(GenerationAction.Refresh(job.id)) },
                    enabled = !busy,
                ) { Text(stringResource(R.string.generation_refresh)) }
            }
        }
    }
}

@Composable
internal fun HomeGeneration(
    onProgress: (String) -> Unit,
    onRead: (String) -> Unit,
) {
    val vm: GenerationViewModel = koinViewModel()
    MviScreen(vm, onEffect = { if (it is GenerationEffect.Read) onRead(it.id.value) }) { state, dispatch ->
        state.homeJob?.let { job ->
            Column {
                GenerationCard(job, state.busy, dispatch)
                TextButton(onClick = { onProgress(job.id.value) }) { Text(stringResource(R.string.generation_progress)) }
                state.error?.let { Text(it.resolve()) }
            }
        }
    }
}
