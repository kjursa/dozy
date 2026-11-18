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
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

class DozyClient private constructor(
    httpClient: HttpClient,
    config: DozyClientConfig,
    installationIdProvider: InstallationIdProvider,
    sessionStorage: AuthSessionStorage,
    private val enableAuthentication: Boolean,
) : AutoCloseable {
    constructor(httpClient: HttpClient, config: DozyClientConfig, installationIdProvider: InstallationIdProvider) :
        this(httpClient, config, installationIdProvider, InMemoryAuthSessionStorage(), false)

    constructor(httpClient: HttpClient, config: DozyClientConfig, installationIdProvider: InstallationIdProvider, sessionStorage: AuthSessionStorage) :
        this(httpClient, config, installationIdProvider, sessionStorage, true)

    private val authClient = KtorAuthClient(httpClient, config)
    val sessionManager = AuthSessionManager(authClient, sessionStorage)
    val auth: AuthClients = DefaultAuthClients(ManagedAuthClient(authClient, sessionManager))
    private val authenticatedHttp = if (enableAuthentication) httpClient.config {
        install(Auth) {
            bearer {
                loadTokens {
                    sessionManager.currentTokens()?.let { BearerTokens(it.accessToken, it.refreshToken) }
                }
                refreshTokens {
                    sessionManager.refreshForRequest()?.let { access ->
                        sessionManager.currentTokens()?.let { BearerTokens(access, it.refreshToken) }
                    }
                }
                sendWithoutRequest { true }
            }
        }
    } else httpClient
    val storyGenerations: StoryGenerationsClients = DefaultStoryGenerationsClients(KtorStoryGenerationsClient(authenticatedHttp, config, installationIdProvider))
    val stories: StoriesClients = DefaultStoriesClients(KtorStoriesClient(authenticatedHttp, config, installationIdProvider))
    val characterAvatars: CharacterAvatarsClients = DefaultCharacterAvatarsClients(KtorCharacterAvatarsClient(authenticatedHttp, config, installationIdProvider))
    val characters: CharactersClients = DefaultCharactersClients(KtorCharactersClient(authenticatedHttp, config, installationIdProvider))
    val places: PlacesClients = DefaultPlacesClients(KtorPlacesClient(authenticatedHttp, config, installationIdProvider))

    override fun close() {
        if (enableAuthentication) authenticatedHttp.close()
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

fun interface InstallationIdProvider {
    fun get(): String
}

class ApiDisabledException : IllegalStateException("Dozy API is disabled")
