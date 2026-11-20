package com.dozy.mobile.feature.login.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.dozy.mobile.resources.login_account_note
import com.dozy.mobile.resources.login_address_intro
import com.dozy.mobile.resources.login_address_title
import com.dozy.mobile.resources.login_avatar
import com.dozy.mobile.resources.login_continue_as_guest
import com.dozy.mobile.resources.login_email_error
import com.dozy.mobile.resources.login_email_intro
import com.dozy.mobile.resources.login_email_label
import com.dozy.mobile.resources.login_email_placeholder
import com.dozy.mobile.resources.login_email_title
import com.dozy.mobile.resources.login_or
import com.dozy.mobile.resources.login_pin_clear
import com.dozy.mobile.resources.login_pin_delete_description
import com.dozy.mobile.resources.login_pin_intro
import com.dozy.mobile.resources.login_pin_resend
import com.dozy.mobile.resources.login_pin_title
import com.dozy.mobile.resources.login_place
import com.dozy.mobile.resources.login_request_error
import com.dozy.mobile.resources.login_send_pin
import com.dozy.mobile.resources.login_sending
import com.dozy.mobile.resources.login_story
import com.dozy.mobile.resources.login_terms
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
import com.dozy.mobile.resources.login_use_email
import com.dozy.mobile.resources.login_verifying
import com.dozy.mobile.ui.common.DozyActionButton
import com.dozy.mobile.ui.common.DozyFootnote
import com.dozy.mobile.ui.common.DozyFormField
import com.dozy.mobile.ui.common.DozyLayout
import com.dozy.mobile.ui.theme.DozyColors
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
            .padding(horizontal = DozyLayout.screenGutter, vertical = DozyLayout.contentGap),
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
            style = MaterialTheme.typography.labelSmall,
            color = DozyColors.textLink,
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
                        .background(if (selected) DozyColors.navigationAccent else MaterialTheme.colorScheme.outline)
                        .clickable { coroutineScope.launch { pagerState.animateScrollToPage(page) } },
                )
            }
        }
        DozyActionButton(onClick = onContinue) {
            Text(stringResource(Res.string.login_tutorial_continue))
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
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = DozyLayout.screenGutter, vertical = DozyLayout.cardPadding),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                stringResource(Res.string.login_address_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                stringResource(Res.string.login_address_intro),
                modifier = Modifier.padding(top = DozyLayout.labelGap, bottom = DozyLayout.screenGutter),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DozyFormField(
                label = stringResource(Res.string.login_email_label),
                value = state.email,
                onValueChange = onEmailChanged,
                placeholder = stringResource(Res.string.login_email_placeholder),
                singleLine = true,
                error = if (state.emailError) stringResource(Res.string.login_email_error) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            )
            if (state.loginError) {
                Text(
                    stringResource(Res.string.login_request_error),
                    modifier = Modifier.padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                stringResource(Res.string.login_account_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        DozyActionButton(onClick = onSubmit, enabled = !state.sendingPin) {
            Text(stringResource(if (state.sendingPin) Res.string.login_sending else Res.string.login_send_pin))
        }
        DozyFootnote(stringResource(Res.string.login_terms), Modifier.padding(top = DozyLayout.labelGap))
    }
}

@Composable
internal fun LoginPinScreen(
    state: LoginUiState,
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
            .padding(horizontal = DozyLayout.screenGutter, vertical = 14.dp),
    ) {
        Text(stringResource(Res.string.login_pin_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(Res.string.login_pin_intro, state.email),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.loginError) {
            Text(
                stringResource(Res.string.login_request_error),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        PinIndicator(state.pin, state.verifyingPin, Modifier.padding(top = 28.dp))
        TextButton(
            onClick = onResend,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !state.sendingPin && !state.verifyingPin,
        ) {
            Text(stringResource(Res.string.login_pin_resend), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        if (state.verifyingPin) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            Text(
                stringResource(Res.string.login_verifying),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center,
            )
        }
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
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 4.dp
        val separatorWidth = 12.dp
        val slotSize = ((maxWidth - gap * 6 - separatorWidth * 2) / 9).coerceAtMost(30.dp)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { group ->
                if (group > 0) {
                    Text(
                        "–",
                        modifier = Modifier.width(separatorWidth),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(3) { position ->
                        val index = group * 3 + position
                        val filled = index < pin.length
                        val focused = index == pin.length && !verifying
                        val borderColor =
                            when {
                                focused -> DozyColors.focus
                                filled -> MaterialTheme.colorScheme.onSurface
                                else -> DozyColors.fieldOutline
                            }
                        Box(
                            Modifier.size(slotSize).border(if (focused) 2.dp else 1.dp, borderColor, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (filled) {
                                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                            }
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) { row ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { column ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when {
                            row < 3 -> {
                                val digit = row * 3 + column + 1
                                NumberKey(digit.toString(), enabled, onClick = { onDigit(digit) })
                            }
                            column == 0 ->
                                TextButton(onClick = onClear, enabled = enabled) {
                                    Text(stringResource(Res.string.login_pin_clear))
                                }
                            column == 1 -> NumberKey("0", enabled, onClick = { onDigit(0) })
                            else ->
                                IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.size(58.dp)) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = stringResource(Res.string.login_pin_delete_description),
                                    )
                                }
                        }
                    }
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
                .size(58.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .78f),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private const val TUTORIAL_PAGE_COUNT = 3

@Composable
internal fun LoginWelcomeScreen(
    onEmail: () -> Unit,
    onGuest: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(DozyLayout.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DozyLayout.contentGap),
    ) {
        Image(
            painterResource(Res.drawable.login_story),
            null,
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 20.dp)
                .size(148.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(stringResource(Res.string.login_email_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(Res.string.login_email_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(Res.string.login_account_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        DozyActionButton(onClick = onEmail) {
            Text(stringResource(Res.string.login_use_email))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f))
            Text(stringResource(Res.string.login_or), style = MaterialTheme.typography.labelSmall)
            HorizontalDivider(Modifier.weight(1f))
        }
        TextButton(onClick = onGuest, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(Res.string.login_continue_as_guest))
        }
        DozyFootnote(stringResource(Res.string.login_terms))
    }
}
