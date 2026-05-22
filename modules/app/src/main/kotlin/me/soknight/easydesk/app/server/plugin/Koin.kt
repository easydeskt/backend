package me.soknight.easydesk.app.server.plugin

import io.ktor.server.application.*
import me.soknight.easydesk.app.EasyDeskApp
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.plugin.module.dsl.withConfiguration

/** Installs [Koin] with SLF4J logging and all application modules. */
fun Application.configureKoin() {
    val config = environment.config
    install(Koin) {
        slf4jLogger()
        modules(module { single { config } })
        withConfiguration<EasyDeskApp>()
    }
}
