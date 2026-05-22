package me.soknight.easydesk.core.server

import io.ktor.server.routing.*

/**
 * Interface for modular Ktor routing registration.
 *
 * Implementations are explicitly registered via Koin modules in the `:app` module
 * and injected at startup using [org.koin.core.Koin.getAll].
 *
 * Each module provides its own routes under a versioned API prefix.
 */
interface ServerModule {

    /** Registers this module's routes within the given [route][Route] scope. */
    fun Route.configureRoutes()

}
