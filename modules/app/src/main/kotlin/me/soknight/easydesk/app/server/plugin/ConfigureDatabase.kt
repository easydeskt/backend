package me.soknight.easydesk.app.server.plugin

import io.ktor.server.application.*
import me.soknight.easydesk.core.persistence.DatabaseConfig
import me.soknight.easydesk.core.persistence.initDatabase

/**
 * Reads database connection parameters from the `easydesk.database` section of `application.yaml`,
 * then bootstraps HikariCP, runs Flyway migrations, and establishes an Exposed database connection.
 *
 * Must be called before [configureKoin] so the connection is ready before any repository bean
 * attempts to open a transaction.
 *
 * Expected configuration keys (all resolvable from environment variables):
 * - `url` — JDBC connection URL (e.g. `jdbc:postgresql://localhost:5432/easydesk`)
 * - `username` — database username
 * - `password` — database password
 * - `maxPoolSize` — HikariCP maximum pool size (default `10`)
 * - `minIdle` — HikariCP minimum idle connections (default `2`)
 *
 * @see initDatabase
 */
fun Application.configureDatabase() {
    val raw = environment.config.config("easydesk.database")

    initDatabase(DatabaseConfig(
        maxPoolSize = raw.property("maxPoolSize").getString().toInt(),
        minIdle = raw.property("minIdle").getString().toInt(),
        password = raw.property("password").getString(),
        url = raw.property("url").getString(),
        username = raw.property("username").getString(),
    ))
}
