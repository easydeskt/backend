package me.soknight.easydesk.api.config

import io.ktor.server.config.*
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
class WorkspaceConfig(@Provided config: ApplicationConfig) {

    private val workspace = config.config("easydesk.workspace")
    private val versionsConfig = workspace.config("versions")

    val backendVersion: String = versionsConfig.property("backend").getString()
    val miniAppVersion: String = versionsConfig.property("mini-app").getString()
    val name: String = workspace.property("name").getString()

}
