package com.dozy.mobile.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.core.config.isAllowedAssetUrl
import org.koin.compose.koinInject

internal fun remoteImage(
    url: String?,
    allowLocalHttp: Boolean = false,
): String? = url?.takeIf { isAllowedAssetUrl(it, allowLocalHttp) }

@Composable
internal fun AvatarImage(
    url: String?,
    description: String,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = remoteImage(url, koinInject<ApiConfig>().allowLocalAssetHttp),
        imageLoader = koinInject(),
        contentDescription = description,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = { Icon(Icons.Default.Face, description) },
        error = { Icon(Icons.Default.Face, description) },
    )
}
