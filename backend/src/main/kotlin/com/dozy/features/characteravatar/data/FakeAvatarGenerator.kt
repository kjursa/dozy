package com.dozy.features.characteravatar.data

import com.dozy.config.AvatarConfig
import com.dozy.config.FakeAvatarMode
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.repository.AvatarGenerator
import kotlinx.coroutines.delay
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class FakeAvatarGenerator(private val config: AvatarConfig) : AvatarGenerator {
    override suspend fun generate(input: AvatarInput): ByteArray {
        delay(config.delayMillis)
        check(config.mode == FakeAvatarMode.SUCCESS) { "Configured generation failure" }
        val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        val color = 0xff000000.toInt() or ((input.type + input.personality + input.appearance).hashCode() and 0x00ffffff)
        for (x in 0 until 64) for (y in 0 until 64) image.setRGB(x, y, color)
        return ByteArrayOutputStream().use { output -> ImageIO.write(image, "png", output); output.toByteArray() }
    }
}
