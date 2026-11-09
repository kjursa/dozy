package com.dozy.mobile.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dozy.mobile.presentation.stories.Story
import com.kjursa.composeuikit.theme.UiSpacing

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
            "See all",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 3.dp).clickable(onClick = onSeeAll),
        )
    }
}

@Composable
internal fun StoryCard(
    story: Story,
    modifier: Modifier = Modifier,
    onClick: (Story) -> Unit = {},
) {
    Surface(
        modifier = modifier.shadow(8.dp, MaterialTheme.shapes.large).clickable { onClick(story) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Column(Modifier.padding(UiSpacing.x2), verticalArrangement = Arrangement.spacedBy(UiSpacing.x2)) {
            Image(
                painterResource(story.image),
                contentDescription = "${story.title} cover",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            3f / 4f,
                        ).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.padding(horizontal = UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(
                    story.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ReadTime(story.minutes)
            }
        }
    }
}

@Composable
internal fun StoryWideCard(
    story: Story,
    onClick: (Story) -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, MaterialTheme.shapes.large).clickable { onClick(story) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
    ) {
        Row(
            Modifier.padding(UiSpacing.x2),
            horizontalArrangement = Arrangement.spacedBy(UiSpacing.x2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(story.image),
                contentDescription = "${story.title} cover",
                modifier =
                    Modifier
                        .width(96.dp)
                        .aspectRatio(
                            3f / 4f,
                        ).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(end = UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(story.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                ReadTime(story.minutes)
            }
        }
    }
}

@Composable
internal fun ReadTime(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
