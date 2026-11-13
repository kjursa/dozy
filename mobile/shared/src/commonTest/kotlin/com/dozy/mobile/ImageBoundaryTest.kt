package com.dozy.mobile

import com.dozy.mobile.core.presentation.components.bundledAvatar
import com.dozy.mobile.core.presentation.components.remoteImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ImageBoundaryTest {
    @Test fun persistedFakeAvatarsUseBundledResources() {
        listOf("oliver", "luna", "finn").forEach {
            assertNotNull(bundledAvatar("bundled-avatar://character_$it"))
            assertNull(remoteImage("bundled-avatar://character_$it"))
        }
        assertNull(bundledAvatar("bundled-avatar://unknown"))
    }

    @Test fun onlyHttpsAssetsReachTheNetworkWithoutRewritingSignedUrls() {
        val cover = "https://example.com/cover.png?token=a%2Fb&v=1"
        assertEquals(cover, remoteImage(cover))
        listOf(null, "", "http://example.com/a", "file:///a", "content://a").forEach {
            assertNull(remoteImage(it))
        }
    }
}
