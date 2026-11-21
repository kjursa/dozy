package com.dozy.mobile.feature.story.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.story_cover
import com.dozy.mobile.resources.story_open
import com.dozy.mobile.resources.story_read_minutes
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
    val title = story.title.resolve()
    Surface(
        onClick = { onClick(story) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AvatarImage(
                story.coverUrl,
                description = stringResource(Res.string.story_cover, title),
                modifier = Modifier.size(width = 94.dp, height = 133.dp).clip(RoundedCornerShape(17.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Black),
                )
                Text(
                    pluralStringResource(Res.plurals.story_read_minutes, story.minutes, story.minutes),
                    Modifier.padding(top = 7.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.story_open),
                    Modifier.padding(top = 9.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.secondary,
                )
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
