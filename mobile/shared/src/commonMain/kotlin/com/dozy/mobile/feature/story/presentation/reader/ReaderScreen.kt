@file:OptIn(ExperimentalComposeUiApi::class)

package com.dozy.mobile.feature.story.presentation.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.generation_back
import com.dozy.mobile.resources.reader_adventure
import com.dozy.mobile.resources.reader_close
import com.dozy.mobile.resources.reader_end
import com.dozy.mobile.resources.reader_enlarge
import com.dozy.mobile.resources.reader_evening
import com.dozy.mobile.resources.reader_large
import com.dozy.mobile.resources.reader_library
import com.dozy.mobile.resources.reader_medium
import com.dozy.mobile.resources.reader_minutes
import com.dozy.mobile.resources.reader_missing
import com.dozy.mobile.resources.reader_retry
import com.dozy.mobile.resources.reader_settings
import com.dozy.mobile.resources.reader_small
import com.dozy.mobile.resources.reader_unavailable
import com.dozy.mobile.ui.common.DozyActionButton
import com.dozy.mobile.ui.common.DozyLayout
import com.dozy.mobile.ui.common.DozyTextStyles
import com.dozy.mobile.ui.theme.DozyReaderNightColors
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ReaderScreen() {
    val vm: ReaderViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.resumed() }
    BackHandler { vm.dispatch(ReaderAction.Back) }
    val colors = if (state.evening) DozyReaderNightColors else MaterialTheme.colorScheme
    MaterialTheme(colorScheme = colors) {
        ReaderContent(state, vm::dispatch)
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState,
    dispatch: (ReaderAction) -> Unit,
) {
    val scroll = rememberScrollState()
    val background = MaterialTheme.colorScheme.background
    val foreground = MaterialTheme.colorScheme.onBackground
    Surface(color = background, contentColor = foreground) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = { dispatch(ReaderAction.Back) },
                ) { Text(stringResource(Res.string.generation_back), color = foreground) }
                TextButton(
                    onClick = { dispatch(ReaderAction.Settings) },
                ) { Text(stringResource(Res.string.reader_settings), color = foreground) }
            }
            Column(
                Modifier
                    .weight(1f)
                    .then(
                        if (state.story !=
                            null
                        ) {
                            Modifier.verticalScroll(scroll)
                        } else {
                            Modifier
                        },
                    ).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(DozyLayout.screenGutter),
            ) {
                when {
                    state.loading -> CircularProgressIndicator()
                    state.story == null -> {
                        Text(stringResource(if (state.unavailable) Res.string.reader_unavailable else Res.string.reader_missing))
                        Button(onClick = { dispatch(ReaderAction.Retry) }) { Text(stringResource(Res.string.reader_retry)) }
                        TextButton(onClick = { dispatch(ReaderAction.Library) }) { Text(stringResource(Res.string.reader_library)) }
                    }
                    else -> {
                        val story = requireNotNull(state.story)
                        Column(verticalArrangement = Arrangement.spacedBy(DozyLayout.labelGap)) {
                            Text(story.title, style = DozyTextStyles.readerTitle)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(DozyLayout.labelGap)) {
                                val metadataStyle = MaterialTheme.typography.labelMedium
                                val metadataColor = MaterialTheme.colorScheme.onSurfaceVariant
                                Text(
                                    stringResource(Res.string.reader_minutes, story.readingMinutes()),
                                    style = metadataStyle,
                                    color = metadataColor,
                                )
                                Text(
                                    story.configuration.cast.joinToString { it.details.name.value },
                                    style = metadataStyle,
                                    color = metadataColor,
                                )
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(
                                onClickLabel = stringResource(Res.string.reader_enlarge),
                            ) { dispatch(ReaderAction.Cover) },
                        ) {
                            AvatarImage(story.coverUrl, story.title, Modifier.fillMaxWidth().height(260.dp))
                        }
                        val size =
                            when (state.textSize) {
                                ReaderUiState.TextSize.SMALL -> 18
                                ReaderUiState.TextSize.MEDIUM -> 21
                                ReaderUiState.TextSize.LARGE -> 24
                            }
                        Text(story.text, style = DozyTextStyles.readerBody(size), textAlign = TextAlign.Start)
                        ReaderEnding(dispatch)
                    }
                }
            }
        }
    }
    when (state.overlay) {
        ReaderUiState.Overlay.SETTINGS -> ReaderSettings(state, dispatch)
        ReaderUiState.Overlay.COVER ->
            state.story?.let { story ->
                Dialog(
                    onDismissRequest = { dispatch(ReaderAction.Dismiss) },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Surface(Modifier.fillMaxSize(), color = background, contentColor = foreground) {
                        Column(Modifier.safeDrawingPadding()) {
                            TextButton(onClick = { dispatch(ReaderAction.Dismiss) }) { Text(stringResource(Res.string.reader_close)) }
                            AvatarImage(story.coverUrl, story.title, Modifier.fillMaxWidth().weight(1f))
                        }
                    }
                }
            }
        null -> Unit
    }
}

@Composable
private fun ReaderEnding(dispatch: (ReaderAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DozyLayout.contentGap),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(Res.string.reader_end),
            modifier = Modifier.fillMaxWidth(),
            style = DozyTextStyles.readerEnding,
            textAlign = TextAlign.Center,
        )
        DozyActionButton(onClick = { dispatch(ReaderAction.NewAdventure) }) {
            Text(stringResource(Res.string.reader_adventure), textAlign = TextAlign.Center)
        }
        TextButton(
            onClick = { dispatch(ReaderAction.Library) },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(stringResource(Res.string.reader_library), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ReaderSettings(
    state: ReaderUiState,
    dispatch: (ReaderAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { dispatch(ReaderAction.Dismiss) },
        title = { Text(stringResource(Res.string.reader_settings)) },
        text = {
            Column {
                ReaderUiState.TextSize.entries.forEach { size ->
                    val label =
                        when (size) {
                            ReaderUiState.TextSize.SMALL -> Res.string.reader_small
                            ReaderUiState.TextSize.MEDIUM -> Res.string.reader_medium
                            ReaderUiState.TextSize.LARGE -> Res.string.reader_large
                        }
                    Row(Modifier.fillMaxWidth().clickable { dispatch(ReaderAction.Size(size)) }) {
                        RadioButton(selected = state.textSize == size, onClick = { dispatch(ReaderAction.Size(size)) })
                        Text(stringResource(label), Modifier.padding(top = 12.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(Res.string.reader_evening), Modifier.weight(1f))
                    Switch(checked = state.evening, onCheckedChange = { dispatch(ReaderAction.Evening(it)) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { dispatch(ReaderAction.Dismiss) }) { Text(stringResource(Res.string.reader_close)) } },
    )
}
