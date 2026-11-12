package com.dozy.mobile.feature.story.presentation.generation

/** Explicit notification intents carry only a validated story identifier. */
fun notificationStoryId(uri: String?): String? =
    uri?.let { Regex("^dozy://stories/([A-Za-z0-9_-]{1,128})$").matchEntire(it)?.groupValues?.get(1) }
