package com.dozy.features.characteravatar.data

import com.dozy.config.AvatarConfig
import com.dozy.config.FakeAvatarMode
import com.dozy.features.characteravatar.domain.model.AvatarInput
import com.dozy.features.characteravatar.domain.repository.AvatarGenerator
import kotlinx.coroutines.delay

class FakeAvatarGenerator(private val config: AvatarConfig) : AvatarGenerator {
    private val fixtures = listOf("fox", "owl", "rabbit")

    override suspend fun generate(input: AvatarInput): ByteArray {
        delay(config.delayMillis)
        check(config.mode == FakeAvatarMode.SUCCESS) { "Configured generation failure" }
        val fixture = fixtures.random()
        return requireNotNull(javaClass.getResourceAsStream("/fixtures/avatars/$fixture.png")).use { it.readBytes() }
    }
}
