package com.dozy.mobile.feature.story.presentation.generation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.core.presentation.components.Eyebrow
import com.dozy.mobile.feature.story.domain.model.GenerationJob
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.back
import com.dozy.mobile.resources.generation_background
import com.dozy.mobile.resources.generation_background_footnote
import com.dozy.mobile.resources.generation_cast_description
import com.dozy.mobile.resources.generation_cast_title
import com.dozy.mobile.resources.generation_cover_description
import com.dozy.mobile.resources.generation_cover_title
import com.dozy.mobile.resources.generation_enable_notifications
import com.dozy.mobile.resources.generation_eyebrow
import com.dozy.mobile.resources.generation_intro_description
import com.dozy.mobile.resources.generation_intro_title
import com.dozy.mobile.resources.generation_read
import com.dozy.mobile.resources.generation_refresh
import com.dozy.mobile.resources.generation_text_description
import com.dozy.mobile.resources.generation_text_title
import com.dozy.mobile.resources.generation_toolbar
import com.dozy.mobile.resources.generation_unavailable
import com.dozy.mobile.resources.place_retry
import com.dozy.mobile.resources.story_back_home
import com.dozy.mobile.ui.common.DozyActionButton
import com.dozy.mobile.ui.common.DozyFootnote
import com.dozy.mobile.ui.common.DozyLayout
import com.dozy.mobile.ui.common.DozySecondaryButton
import com.dozy.mobile.ui.theme.DozyColors
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.StringResource
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
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .drawBehind {
                    drawRect(Brush.radialGradient(
                        listOf(DozyColors.brandTeal.copy(alpha = .075f), Color.Transparent),
                        Offset(size.width, size.height * .03f),
                        size.width * .8f,
                    ))
                    drawRect(Brush.radialGradient(
                        listOf(DozyColors.brandCoral.copy(alpha = .043f), Color.Transparent),
                        Offset(0f, size.height * .18f),
                        size.width * .65f,
                    ))
                }.statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            GenerationToolbar(onHome)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = DozyLayout.screenGutter).padding(top = 12.dp, bottom = 35.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GenerationIllustration()
                Eyebrow(stringResource(Res.string.generation_eyebrow))
                Text(
                    stringResource(Res.string.generation_intro_title),
                    Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(Res.string.generation_intro_description),
                    Modifier.padding(top = 18.dp, bottom = 30.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                val job = state.jobs.find { it.id.value == jobId }
                when {
                    job != null -> GenerationCard(job, state.busy, dispatch)
                    state.loading -> CircularProgressIndicator()
                    state.error == null -> Text(stringResource(Res.string.generation_unavailable))
                }
                state.error?.let { Text(it.resolve(), Modifier.padding(top = 12.dp)) }
                if (backgroundCompletionSupported) {
                    Surface(
                        Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = DozyColors.informationBackground,
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                stringResource(Res.string.generation_background),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (requestNotifications != null) {
                                TextButton(onClick = requestNotifications) {
                                    Text(stringResource(Res.string.generation_enable_notifications))
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    if (requestNotifications != null) {
                        TextButton(onClick = requestNotifications) {
                            Text(stringResource(Res.string.generation_enable_notifications))
                        }
                    }
                }
                DozyActionButton(onClick = onHome) {
                    Text(stringResource(Res.string.story_back_home), fontWeight = FontWeight.Black)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(start = 12.dp).size(20.dp))
                }
                if (backgroundCompletionSupported) {
                    DozyFootnote(stringResource(Res.string.generation_background_footnote), Modifier.padding(top = 13.dp))
                }
            }
        }
    }
}

@Composable
private fun GenerationToolbar(onHome: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, top = 12.dp, end = 22.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onHome,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), Modifier.size(21.dp))
            }
        }
        Text(
            stringResource(Res.string.generation_toolbar),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun GenerationIllustration() {
    val mint = Color(0xFFE3F2E9)
    Box(
        Modifier.padding(bottom = 8.dp).size(238.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(238.dp).background(mint.copy(alpha = .15f), CircleShape))
        Box(Modifier.size(206.dp).background(mint.copy(alpha = .3f), CircleShape))
        Box(Modifier.size(174.dp).background(mint, CircleShape)) {
            Icon(
                Icons.Default.AutoStories,
                null,
                Modifier.align(Alignment.Center).size(68.dp).rotate(-10f),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
            )
            Icon(
                Icons.Default.AutoAwesome,
                null,
                Modifier.align(Alignment.TopEnd).offset(x = (-9).dp).size(38.dp),
                tint = DozyColors.brandCoral,
            )
            Icon(
                Icons.Default.AutoAwesome,
                null,
                Modifier.align(Alignment.BottomStart).offset(x = 3.dp, y = (-13).dp).size(27.dp),
                tint = Color(0xFFBB98BD),
            )
        }
    }
}

@Composable
internal fun GenerationCard(
    job: GenerationJob,
    busy: Boolean,
    dispatch: (GenerationAction) -> Unit,
) {
    val currentStep = when (job.status) {
        GenerationJob.Status.QUEUED, GenerationJob.Status.CREATING_TEXT -> 2
        GenerationJob.Status.CREATING_COVER -> 3
        GenerationJob.Status.READY -> 4
        GenerationJob.Status.FAILED -> 2
    }
    val unavailable = job.statusUnknown || job.retryPending
    val failed = job.status == GenerationJob.Status.FAILED
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(24.dp)) {
            GenerationStep(1, currentStep, false, Res.string.generation_cast_title, Res.string.generation_cast_description)
            GenerationStep(2, currentStep, !unavailable && !failed, Res.string.generation_text_title, Res.string.generation_text_description)
            GenerationStep(3, currentStep, !unavailable && !failed, Res.string.generation_cover_title, Res.string.generation_cover_description)
            if (unavailable || failed) {
                Text(job.message().resolve(), Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
            if (job.storyId != null) {
                DozySecondaryButton(enabled = !busy, onClick = { dispatch(GenerationAction.Open(job.id)) }) {
                    Text(stringResource(Res.string.generation_read))
                }
            } else {
                if (failed && !unavailable) {
                    DozySecondaryButton(enabled = !busy, onClick = { dispatch(GenerationAction.Retry(job.id)) }) {
                        Text(stringResource(Res.string.place_retry))
                    }
                }
                TextButton(enabled = !busy, onClick = { dispatch(GenerationAction.Refresh(job.id)) }) {
                    Text(stringResource(Res.string.generation_refresh))
                }
            }
        }
    }
}

@Composable
private fun GenerationStep(
    number: Int,
    currentStep: Int,
    showProgress: Boolean,
    title: StringResource,
    description: StringResource,
) {
    val done = number < currentStep
    val active = number == currentStep && showProgress
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            if (active) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                    strokeWidth = 2.dp,
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        if (done) MaterialTheme.colorScheme.secondaryContainer else Color(0xFFF4EFE7),
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    } else {
                        Text(number.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Column {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (active || done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(description),
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
