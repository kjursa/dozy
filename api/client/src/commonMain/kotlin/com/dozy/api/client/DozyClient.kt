package com.dozy.api.client

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
    httpClient: HttpClient,
    config: DozyClientConfig,
    installationIdProvider: InstallationIdProvider,
) {
    val storyGenerations: StoryGenerationsClients = DefaultStoryGenerationsClients(KtorStoryGenerationsClient(httpClient, config, installationIdProvider))
    val stories: StoriesClients = DefaultStoriesClients(KtorStoriesClient(httpClient, config, installationIdProvider))
    val characters: CharactersClients = DefaultCharactersClients(KtorCharactersClient(httpClient, config, installationIdProvider))
    val places: PlacesClients = DefaultPlacesClients(KtorPlacesClient(httpClient, config, installationIdProvider))
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
