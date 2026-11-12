package com.dozy.mobile.feature.story.presentation.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ReaderScreen(onEffect: (ReaderEffect) -> Unit) {
    val vm: ReaderViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val currentEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(vm) { vm.effects.collect { currentEffect(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.resumed() }
    BackHandler { vm.dispatch(ReaderAction.Back) }
    ReaderContent(state, vm::dispatch)
}

@Composable
private fun ReaderContent(
    state: ReaderUiState,
    dispatch: (ReaderAction) -> Unit,
) {
    val scroll = rememberScrollState()
    val background = if (state.evening) Color(0xFF242638) else MaterialTheme.colorScheme.background
    val foreground = if (state.evening) Color(0xFFF1E9DA) else MaterialTheme.colorScheme.onBackground
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
                verticalArrangement = Arrangement.spacedBy(24.dp),
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
                        Text(story.title, style = MaterialTheme.typography.headlineLarge)
                        Text(story.configuration.cast.joinToString { it.details.name.value })
                        Text(stringResource(Res.string.reader_minutes, story.readingMinutes()))
                        Box(
                            Modifier.fillMaxWidth().clickable(
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
                        Text(story.text, style = MaterialTheme.typography.bodyLarge, fontSize = size.sp, lineHeight = (size * 1.6).sp)
                        Text(stringResource(Res.string.reader_end), style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { dispatch(ReaderAction.NewAdventure) }) { Text(stringResource(Res.string.reader_adventure)) }
                        TextButton(onClick = { dispatch(ReaderAction.Library) }) { Text(stringResource(Res.string.reader_library)) }
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
