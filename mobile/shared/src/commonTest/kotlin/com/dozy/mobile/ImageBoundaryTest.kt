package com.dozy.mobile

import com.dozy.mobile.core.presentation.components.remoteImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageBoundaryTest {
    @Test fun onlyHttpsAssetsReachTheNetworkWithoutRewritingSignedUrls() {
        val cover = "https://example.com/cover.png?token=a%2Fb&v=1"
        assertEquals(cover, remoteImage(cover))
        listOf(null, "", "http://example.com/a", "file:///a", "content://a", "bundled-avatar://character_luna").forEach {
            assertNull(remoteImage(it))
        }
    }

    @Test fun acceptsEmulatorPlaceThumbnailsOnlyWithLocalHttpEnabled() {
        val place = "http://10.0.2.2:8080/api/v1/place-icons/forest.png"
        assertEquals(place, remoteImage(place, allowLocalHttp = true))
        assertNull(remoteImage(place))
    }
}
