package me.soknight.easydesk.app.server.plugin

import io.ktor.server.application.*
import kotlinx.io.files.Path
import me.soknight.easydesk.app.EasyDeskApp
import me.soknight.easydesk.service.storage.config.StorageConfig
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.plugin.module.dsl.withConfiguration

/** Installs [Koin] with SLF4J logging and all application modules. */
fun Application.configureKoin() {
    val config = environment.config
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { config }
            single {
                val rootPath = config.propertyOrNull("easydesk.storage.rootPath")?.getString()
                    ?: "./data/attachments"
                StorageConfig(Path(rootPath))
            }
        })
        withConfiguration<EasyDeskApp>()
    }
}
