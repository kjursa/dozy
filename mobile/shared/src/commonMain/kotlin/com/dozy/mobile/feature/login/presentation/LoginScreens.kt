package com.dozy.mobile.feature.login.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.dozy_cloud_logo
import com.dozy.mobile.resources.login_avatar
import com.dozy.mobile.resources.login_continue_as_guest
import com.dozy.mobile.resources.login_email_error
import com.dozy.mobile.resources.login_email_intro
import com.dozy.mobile.resources.login_email_label
import com.dozy.mobile.resources.login_email_placeholder
import com.dozy.mobile.resources.login_email_title
import com.dozy.mobile.resources.login_pin_back
import com.dozy.mobile.resources.login_pin_check_mail
import com.dozy.mobile.resources.login_pin_clear
import com.dozy.mobile.resources.login_pin_delete_description
import com.dozy.mobile.resources.login_pin_intro
import com.dozy.mobile.resources.login_pin_resend
import com.dozy.mobile.resources.login_pin_title
import com.dozy.mobile.resources.login_place
import com.dozy.mobile.resources.login_send_pin
import com.dozy.mobile.resources.login_sending
import com.dozy.mobile.resources.login_story
import com.dozy.mobile.resources.login_tutorial_avatar_description
import com.dozy.mobile.resources.login_tutorial_avatar_title
import com.dozy.mobile.resources.login_tutorial_continue
import com.dozy.mobile.resources.login_tutorial_kicker
import com.dozy.mobile.resources.login_tutorial_place_description
import com.dozy.mobile.resources.login_tutorial_place_title
import com.dozy.mobile.resources.login_tutorial_story_description
import com.dozy.mobile.resources.login_tutorial_story_title
import com.dozy.mobile.resources.login_tutorial_subtitle
import com.dozy.mobile.resources.login_tutorial_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginTutorialScreen(onContinue: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { TUTORIAL_PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(Res.drawable.dozy_cloud_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Text("dozy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            stringResource(Res.string.login_tutorial_kicker),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(Res.string.login_tutorial_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(Res.string.login_tutorial_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            pageSpacing = 12.dp,
        ) { page ->
            TutorialCard(page)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(TUTORIAL_PAGE_COUNT) { page ->
                val selected = pagerState.currentPage == page
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        .clickable { coroutineScope.launch { pagerState.animateScrollToPage(page) } },
                )
            }
        }
        Button(
            onClick = onContinue,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(stringResource(Res.string.login_tutorial_continue), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TutorialCard(page: Int) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f)),
        shadowElevation = 3.dp,
    ) {
        Column {
            when (page) {
                0 ->
                    Image(
                        painter = painterResource(Res.drawable.login_story),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentScale = ContentScale.Crop,
                    )
                1 ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE5F0E4), Color(0xFFF4E8D8), Color(0xFFE6EFE7)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.login_avatar),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(224.dp)
                                    .clip(CircleShape)
                                    .border(5.dp, Color(0xFFFFFAF0), CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                else ->
                    Image(
                        painter = painterResource(Res.drawable.login_place),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        contentScale = ContentScale.Crop,
                    )
            }
            Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp)) {
                val title =
                    when (page) {
                        0 -> stringResource(Res.string.login_tutorial_story_title)
                        1 -> stringResource(Res.string.login_tutorial_avatar_title)
                        else -> stringResource(Res.string.login_tutorial_place_title)
                    }
                val description =
                    when (page) {
                        0 -> stringResource(Res.string.login_tutorial_story_description)
                        1 -> stringResource(Res.string.login_tutorial_avatar_description)
                        else -> stringResource(Res.string.login_tutorial_place_description)
                    }
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun LoginEmailScreen(
    state: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(stringResource(Res.string.login_email_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            stringResource(Res.string.login_email_intro),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.login_email_label)) },
            placeholder = { Text(stringResource(Res.string.login_email_placeholder)) },
            singleLine = true,
            isError = state.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            shape = RoundedCornerShape(16.dp),
        )
        if (state.emailError) {
            Text(
                stringResource(Res.string.login_email_error),
                modifier = Modifier.padding(start = 8.dp, top = 5.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSubmit,
            enabled = !state.sendingPin,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                stringResource(if (state.sendingPin) Res.string.login_sending else Res.string.login_send_pin),
                fontWeight = FontWeight.Bold,
            )
        }
        TextButton(
            onClick = onContinueAsGuest,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp),
        ) {
            Text(stringResource(Res.string.login_continue_as_guest), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun LoginPinScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.login_pin_back))
        }
        Text(
            stringResource(Res.string.login_pin_check_mail),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(stringResource(Res.string.login_pin_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            stringResource(Res.string.login_pin_intro, state.email),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PinIndicator(state.pin, state.verifyingPin, Modifier.padding(top = 28.dp))
        TextButton(
            onClick = onResend,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !state.sendingPin,
        ) {
            Text(stringResource(Res.string.login_pin_resend), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        NumberPad(
            enabled = !state.verifyingPin,
            onDigit = onDigit,
            onDelete = onDelete,
            onClear = onClear,
        )
    }
}

@Composable
private fun PinIndicator(
    pin: String,
    verifying: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { group ->
            if (group > 0) {
                Text("-", modifier = Modifier.padding(horizontal = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { position ->
                    val index = group * 3 + position
                    val filled = index < pin.length
                    val focused = index == pin.length && !verifying
                    Box(
                        Modifier
                            .size(25.dp)
                            .then(
                                if (focused) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (filled) {
                            Text("●", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                row.forEach { digit ->
                    NumberKey(digit.toString(), enabled, onClick = { onDigit(digit) })
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClear, enabled = enabled) { Text(stringResource(Res.string.login_pin_clear)) }
            NumberKey("0", enabled, onClick = { onDigit(0) })
            Surface(
                modifier =
                    Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .clickable(enabled = enabled, onClick = onDelete),
                shape = CircleShape,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(Res.string.login_pin_delete_description),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .78f),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private const val TUTORIAL_PAGE_COUNT = 3
