package com.dozy.mobile.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dozy.mobile.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AmbientGlow(modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(Modifier.padding(top = 5.dp).size(190.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .07f), CircleShape))
        Box(
            Modifier
                .align(
                    Alignment.TopEnd,
                ).offset(x = 90.dp, y = 145.dp)
                .size(220.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = .09f), CircleShape),
        )
        Box(
            Modifier
                .align(
                    Alignment.BottomStart,
                ).offset(
                    x = (-70).dp,
                    y = (-110).dp,
                ).size(180.dp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = .14f), CircleShape),
        )
    }
}

@Composable
internal fun Eyebrow(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SectionHeading(
    kicker: String,
    title: String,
    onSeeAll: () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column {
            Eyebrow(kicker)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            stringResource(Res.string.see_all),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 3.dp).clickable(onClick = onSeeAll),
        )
    }
}
