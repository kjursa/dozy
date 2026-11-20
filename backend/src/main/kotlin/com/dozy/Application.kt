package com.dozy

import com.dozy.features.asset.api.v1.assetV1Routes
import com.dozy.features.auth.api.v1.authV1Routes
import com.dozy.features.asset.domain.repository.AssetRepository
import com.dozy.features.story.application.StoryGenerationScheduler
import com.dozy.features.story.application.StoryService
import com.dozy.features.story.api.v1.storyV1Routes
import com.dozy.features.characteravatar.api.v1.avatarV1Routes
import com.dozy.features.characteravatar.api.v1.avatarAssetRoutes
import com.dozy.features.characteravatar.application.AvatarWorker
import com.dozy.features.characteravatar.domain.repository.AvatarJobs
import com.dozy.features.character.api.v1.characterV1Routes
import com.dozy.api.contracts.ApiVersion
import com.dozy.di.configureDependencies
import com.dozy.features.place.api.v1.placeV1Routes
import com.dozy.features.place.application.PlaceService
import com.dozy.features.auth.application.AccessTokens
import com.dozy.plugins.OWNER_AUTH
import com.dozy.plugins.configureErrors
import com.dozy.plugins.configureOwnerAuthentication
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HelloResponse(val message: String)

fun Application.module() {
    configureDependencies()
    configureErrors()
    val accessTokens: AccessTokens by dependencies
    configureOwnerAuthentication(accessTokens)
    install(CallLogging)
    install(ContentNegotiation) { json(Json { encodeDefaults = true }) }
    val places: PlaceService by dependencies
    val assets: AssetRepository by dependencies
    val stories: StoryService by dependencies
    val storyScheduler: StoryGenerationScheduler by dependencies
    val jobs: AvatarJobs by dependencies
    val avatarWorker: AvatarWorker by dependencies
    storyScheduler.start(this)
    avatarWorker.start(this)
    routing {
        authenticate(OWNER_AUTH) { avatarAssetRoutes(assets) }
        get("/hello") { call.respond(HelloResponse("Hello world from Dozy")) }
        route(ApiVersion.V1.path) {
            authV1Routes()
            authenticate(OWNER_AUTH) {
                assetV1Routes(assets)
                storyV1Routes(stories)
                placeV1Routes(places)
                characterV1Routes()
                avatarV1Routes(jobs)
            }
        }
    }
}

fun main(args: Array<String>) = EngineMain.main(args)
