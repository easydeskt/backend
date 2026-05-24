package me.soknight.easydesk.api.config

import io.ktor.server.config.*
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
class WorkspaceConfig(@Provided config: ApplicationConfig) {

    val name: String = config.config("easydesk.workspace").property("name").getString()

}
