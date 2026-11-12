package com.dozy.mobile.core.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.dozy.mobile.resources.*
import com.kjursa.foundation.coroutines.runCatchingSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Platform image boundary for remote assets and bundled local-preview avatars. */
@Composable
internal fun AvatarImage(
    url: String?,
    description: String,
    modifier: Modifier = Modifier,
) {
    val bundled =
        when (url) {
            "bundled-avatar://character_oliver" -> Res.drawable.character_oliver
            "bundled-avatar://character_luna" -> Res.drawable.character_luna
            "bundled-avatar://character_finn" -> Res.drawable.character_finn
            else -> null
        }
    if (bundled != null) {
        Image(painterResource(bundled), description, modifier, contentScale = ContentScale.Crop)
        return
    }
    val bitmap by produceState<ImageBitmap?>(null, url) {
        value = null
        if (url?.startsWith("https://") == true) {
            value =
                runCatchingSuspend {
                    withContext(Dispatchers.IO) {
                        val connection = URL(url).openConnection() as HttpsURLConnection
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        try {
                            connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                        } finally {
                            connection.disconnect()
                        }
                    }
                }.getOrNull()
        }
    }
    val image = bitmap
    if (image == null) {
        Icon(Icons.Default.Face, description, modifier)
    } else {
        Image(image, description, modifier, contentScale = ContentScale.Fit)
    }
}
