package com.dozy.mobile

import android.content.ContentProvider
import org.robolectric.Robolectric

/** Robolectric does not start manifest providers automatically like the Android framework does. */
internal fun initializeComposeResourceProvider() {
    val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider").asSubclass(ContentProvider::class.java)
    Robolectric.buildContentProvider(provider).create()
}
