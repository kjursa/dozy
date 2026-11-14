package com.dozy

import com.dozy.api.contracts.ApiVersion
import com.dozy.di.configureDependencies
import com.dozy.features.place.api.placeRoutes
import com.dozy.features.place.application.PlaceService
import com.dozy.plugins.configureErrors
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
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
    install(CallLogging)
    install(ContentNegotiation) { json(Json { encodeDefaults = true }) }
    val places: PlaceService by dependencies
    routing {
        get("/hello") { call.respond(HelloResponse("Hello world from Dozy")) }
        route(ApiVersion.V1.path) { placeRoutes(places) }
    }
}

fun main(args: Array<String>) = EngineMain.main(args)
