package com.dozy.mobile.feature.story.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dozy.mobile.core.presentation.components.AvatarImage
import com.dozy.mobile.feature.story.presentation.list.Story
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.theme.UiSpacing
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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
            AvatarImage(
                story.coverUrl,
                description = stringResource(Res.string.story_cover, story.title.resolve()),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            3f / 4f,
                        ).clip(MaterialTheme.shapes.medium),
            )
            Column(Modifier.padding(horizontal = UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(
                    story.title.resolve(),
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
            AvatarImage(
                story.coverUrl,
                description = stringResource(Res.string.story_cover, story.title.resolve()),
                modifier =
                    Modifier
                        .width(96.dp)
                        .aspectRatio(
                            3f / 4f,
                        ).clip(MaterialTheme.shapes.medium),
            )
            Column(Modifier.weight(1f).padding(end = UiSpacing.x1), verticalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
                Text(story.title.resolve(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                ReadTime(story.minutes)
            }
        }
    }
}

@Composable
private fun ReadTime(minutes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(UiSpacing.x1)) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            pluralStringResource(Res.plurals.story_read_minutes, minutes, minutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
