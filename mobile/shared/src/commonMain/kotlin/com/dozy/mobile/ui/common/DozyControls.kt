package com.dozy.mobile.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.ui.theme.DozyColors

@Composable
internal fun DozyActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = DozyLayout.actionMinHeight),
        enabled = enabled,
        shape = RoundedCornerShape(DozyLayout.actionRadius),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
internal fun DozySecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = DozyLayout.actionMinHeight),
        enabled = enabled,
        shape = RoundedCornerShape(DozyLayout.actionRadius),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary,
            ),
        content = content,
    )
}

@Composable
internal fun DozyFootnote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun DozyFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    labelHint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minimumHeight: Dp = OutlinedTextFieldDefaults.MinHeight,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DozyLayout.labelGap)) {
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(DozyLayout.labelGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = typography.titleMedium, color = colors.onSurface)
            labelHint?.let { Text(it, style = typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier.fillMaxWidth().heightIn(min = minimumHeight).semantics {
                    contentDescription = if (labelHint == null) label else "$label, $labelHint"
                },
            enabled = enabled,
            isError = error != null,
            singleLine = singleLine,
            placeholder = placeholder?.let { { Text(it) } },
            supportingText = error?.let { { Text(it, style = typography.bodySmall) } },
            textStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 27.sp, textAlign = TextAlign.Start),
            shape = RoundedCornerShape(if (singleLine) DozyLayout.fieldRadius else DozyLayout.multilineFieldRadius),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DozyColors.focus,
                    unfocusedBorderColor = DozyColors.fieldOutline,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    errorContainerColor = colors.surface,
                    focusedPlaceholderColor = DozyColors.placeholder,
                    unfocusedPlaceholderColor = DozyColors.placeholder,
                    cursorColor = DozyColors.focus,
                ),
        )
    }
}
