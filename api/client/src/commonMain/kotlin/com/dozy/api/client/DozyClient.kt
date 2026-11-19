package com.dozy.api.client

import com.dozy.api.client.characteravatar.CharacterAvatarsClients
import com.dozy.api.client.characteravatar.DefaultCharacterAvatarsClients
import com.dozy.api.client.characteravatar.v1.KtorCharacterAvatarsClient
import com.dozy.api.client.auth.AuthClients
import com.dozy.api.client.auth.AuthSessionManager
import com.dozy.api.client.auth.AuthSessionStorage
import com.dozy.api.client.auth.DefaultAuthClients
import com.dozy.api.client.auth.InMemoryAuthSessionStorage
import com.dozy.api.client.auth.ManagedAuthClient
import com.dozy.api.client.auth.v1.KtorAuthClient
import com.dozy.api.client.character.CharactersClients
import com.dozy.api.client.character.DefaultCharactersClients
import com.dozy.api.client.character.v1.KtorCharactersClient
import com.dozy.api.client.place.DefaultPlacesClients
import com.dozy.api.client.place.PlacesClients
import com.dozy.api.client.place.v1.KtorPlacesClient
import com.dozy.api.client.story.StoriesClients
import com.dozy.api.client.story.DefaultStoriesClients
import com.dozy.api.client.story.v1.KtorStoriesClient
import com.dozy.api.client.storygeneration.StoryGenerationsClients
import com.dozy.api.client.storygeneration.DefaultStoryGenerationsClients
import com.dozy.api.client.storygeneration.v1.KtorStoryGenerationsClient
import com.dozy.api.contracts.ApiVersion
import io.ktor.client.HttpClient

class DozyClient(
    private val httpClient: HttpClient,
    config: DozyClientConfig,
    sessionStorage: AuthSessionStorage = InMemoryAuthSessionStorage(),
) : AutoCloseable {
    private val authClient = KtorAuthClient(httpClient, config)
    val sessionManager = AuthSessionManager(authClient, sessionStorage)
    val auth: AuthClients = DefaultAuthClients(ManagedAuthClient(authClient, sessionManager))
    val assetHttpClient: HttpClient = if (config.baseUrl != null) httpClient.config {
        followRedirects = false
        install(ownerAuthorization(config.baseUrl, sessionManager))
    } else httpClient
    val storyGenerations: StoryGenerationsClients = DefaultStoryGenerationsClients(KtorStoryGenerationsClient(assetHttpClient, config))
    val stories: StoriesClients = DefaultStoriesClients(KtorStoriesClient(assetHttpClient, config))
    val characterAvatars: CharacterAvatarsClients = DefaultCharacterAvatarsClients(KtorCharacterAvatarsClient(assetHttpClient, config))
    val characters: CharactersClients = DefaultCharactersClients(KtorCharactersClient(assetHttpClient, config))
    val places: PlacesClients = DefaultPlacesClients(KtorPlacesClient(assetHttpClient, config))

    override fun close() {
        if (assetHttpClient !== httpClient) assetHttpClient.close()
    }
}

data class DozyClientConfig(
    val baseUrl: String?,
) {
    init {
        require(baseUrl == null || baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))
        require(baseUrl == null || '?' !in baseUrl && '#' !in baseUrl)
    }

    internal fun endpoint(version: ApiVersion, resource: String): String {
        val base = baseUrl ?: throw ApiDisabledException()
        return "${base.trimEnd('/')}${version.path}/$resource"
    }
}

class ApiDisabledException : IllegalStateException("Dozy API is disabled")
