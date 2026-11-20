package com.dozy.config

import java.net.URI

enum class FakeAvatarMode { SUCCESS, FAILURE }
data class AvatarConfig(
    val storagePath: String = "data/assets",
    val publicBaseUrl: String = "http://localhost:8080",
    val allowLocalHttp: Boolean = true,
    val mode: FakeAvatarMode = FakeAvatarMode.SUCCESS,
    val delayMillis: Long = 1000,
    val concurrency: Int = 2,
) {
    init {
        val uri = URI(publicBaseUrl)
        require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null)
        require(uri.scheme == "https" || (allowLocalHttp && uri.scheme == "http" && uri.host in setOf("localhost", "127.0.0.1", "::1", "[::1]", "10.0.2.2")))
        require(storagePath.isNotBlank() && delayMillis >= 0 && concurrency in 1..16)
    }
}
