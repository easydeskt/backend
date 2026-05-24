package me.soknight.easydesk.api.config

import io.ktor.server.config.*
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
class WorkspaceConfig(@Provided config: ApplicationConfig) {

    private val workspace = config.config("easydesk.workspace")

    val backendVersion: String = WorkspaceConfig::class.java.`package`?.implementationVersion ?: "unknown"
    val name: String = workspace.property("name").getString()

}
