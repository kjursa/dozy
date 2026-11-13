package com.dozy.mobile.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.dozy.mobile.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Persisted fake references remain local resources; only HTTPS assets reach the network. */
internal fun bundledAvatar(url: String?): DrawableResource? =
    when (url) {
        "bundled-avatar://character_oliver" -> Res.drawable.character_oliver
        "bundled-avatar://character_luna" -> Res.drawable.character_luna
        "bundled-avatar://character_finn" -> Res.drawable.character_finn
        else -> null
    }

internal fun remoteImage(url: String?): String? = url?.takeIf { it.startsWith("https://") }

@Composable
internal fun AvatarImage(
    url: String?,
    description: String,
    modifier: Modifier = Modifier,
) {
    val bundled = bundledAvatar(url)
    if (bundled != null) {
        Image(painterResource(bundled), description, modifier, contentScale = ContentScale.Crop)
    } else {
        SubcomposeAsyncImage(
            model = remoteImage(url),
            imageLoader = org.koin.compose.koinInject(),
            contentDescription = description,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            loading = { Icon(Icons.Default.Face, description) },
            error = { Icon(Icons.Default.Face, description) },
        )
    }
}
