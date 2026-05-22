package me.soknight.easydesk.app.server.plugin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

/** Installs [ContentNegotiation] with kotlinx JSON serialization. */
fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        json(Json {
            coerceInputValues = true
            encodeDefaults = false
            explicitNulls = false
            ignoreUnknownKeys = true
            prettyPrint = false
        })
    }
}
