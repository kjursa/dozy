package com.dozy.features.story.data

import com.dozy.config.GenerationConfig
import com.dozy.features.story.domain.model.ContentRejectedException
import com.dozy.features.story.domain.model.CoverGenerator
import com.dozy.features.story.domain.model.GeneratedText
import com.dozy.features.story.domain.model.StoryConfiguration
import com.dozy.features.story.domain.model.TextGenerator
import kotlinx.coroutines.delay

class FakeTextGenerator(private val config: GenerationConfig) : TextGenerator {
    private val placeholder = Regex("""\{(heroes|place)\}""")

    override suspend fun generate(configuration: StoryConfiguration): GeneratedText {
        delay(config.delayMillis)
        when (config.textMode) {
            GenerationConfig.TextMode.FAILURE -> error("Configured text failure")
            GenerationConfig.TextMode.CONTENT_REJECTED -> throw ContentRejectedException()
            GenerationConfig.TextMode.FAIL_ONCE, GenerationConfig.TextMode.SUCCESS -> Unit
        }
        val heroes = configuration.cast.joinToString(" and ") { it.name }
        val setting = configuration.place?.name ?: "the quiet moonlit garden"
        val story = Stories.random()
        val text = placeholder.replace(story.substringAfter('\n')) {
            if (it.groupValues[1] == "heroes") heroes else setting
        }
        return GeneratedText(story.substringBefore('\n'), text)
    }
}
class FakeCoverGenerator(private val config: GenerationConfig) : CoverGenerator {
    override suspend fun generate(configuration: StoryConfiguration): ByteArray {
        delay(config.delayMillis)
        check(config.coverMode == GenerationConfig.CoverMode.SUCCESS) { "Configured cover failure" }
        return requireNotNull(javaClass.getResourceAsStream("/fixtures/story-cover.png")).use { it.readBytes() }
    }
}
