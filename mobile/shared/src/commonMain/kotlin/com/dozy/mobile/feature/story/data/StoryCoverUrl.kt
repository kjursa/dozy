package com.dozy.mobile.feature.story.data

import io.ktor.http.URLProtocol
import io.ktor.http.Url

internal fun validStoryCoverUrl(value: String): Boolean {
    val url = runCatching { Url(value) }.getOrNull() ?: return false
    return url.host.isNotBlank() &&
        (
            url.protocol == URLProtocol.HTTPS ||
                url.protocol == URLProtocol.HTTP
        )
}
