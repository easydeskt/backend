package me.soknight.easydesk.api.config

import io.ktor.server.config.*
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
class ApiConfig(@Provided config: ApplicationConfig) {

    val devAuthSkip: Boolean = config.config("easydesk.api").property("devAuthSkip").getString().toBoolean()

}
