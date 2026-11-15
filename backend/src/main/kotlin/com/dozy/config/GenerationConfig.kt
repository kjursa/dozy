package com.dozy.config

import io.ktor.server.config.ApplicationConfig
import java.net.URI

data class GenerationConfig(
    val textMode: TextMode = TextMode.SUCCESS,
    val coverMode: CoverMode = CoverMode.SUCCESS,
    val delayMillis: Long = 250,
    val concurrency: Int = 2,
    val assetBaseUrl: String = "https://localhost:8080",
    val allowHttp: Boolean = false,
    val leaseMillis: Long = 30000,
) {
    enum class TextMode { SUCCESS, FAILURE, CONTENT_REJECTED, FAIL_ONCE }
    enum class CoverMode { SUCCESS, FAILURE }
    init {
        require(delayMillis >= 0 && concurrency in 1..32 && leaseMillis >= 300)
        val uri = URI(assetBaseUrl)
        require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null)
        require(uri.scheme == "https" || allowHttp && uri.scheme == "http")
    }
    companion object {
        fun from(config: ApplicationConfig): GenerationConfig {
            fun value(name: String) = config.propertyOrNull("dozy.generation.$name")?.getString()
            return GenerationConfig(
                textMode = value("textMode")?.let(TextMode::valueOf) ?: TextMode.SUCCESS,
                coverMode = value("coverMode")?.let(CoverMode::valueOf) ?: CoverMode.SUCCESS,
                delayMillis = value("delayMillis")?.toLong() ?: 250,
                concurrency = value("concurrency")?.toInt() ?: 2,
                assetBaseUrl = value("assetBaseUrl") ?: "https://localhost:8080",
                allowHttp = value("allowHttp")?.toBooleanStrict() ?: false,
            )
        }
    }
}
