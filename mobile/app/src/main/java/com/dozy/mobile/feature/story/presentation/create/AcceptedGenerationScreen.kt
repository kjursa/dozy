package com.dozy.mobile.feature.story.presentation.create

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dozy.mobile.R

/** F10 entry point. Acceptance is the only known fact until observation is implemented. */
@Composable
internal fun AcceptedGenerationScreen(
    jobId: String,
    onHome: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.story_job_accepted))
        Text(stringResource(R.string.story_job_reference, jobId))
        Button(onClick = onHome) { Text(stringResource(R.string.story_back_home)) }
    }
}
