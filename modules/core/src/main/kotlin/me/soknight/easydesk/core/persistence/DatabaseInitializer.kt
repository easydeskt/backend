package me.soknight.easydesk.core.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.soknight.easydesk.core.logging.getLogger
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

private val logger = getLogger()

/**
 * Bootstraps the application database: creates a HikariCP connection pool,
 * runs Flyway migrations against schema `public`, and establishes an Exposed [Database] connection.
 *
 * Call once at application startup, before any repository opens a transaction.
 *
 * @param config connection and pool parameters
 * @return the connected Exposed [Database] instance
 * @see DatabaseConfig
 */
fun initDatabase(config: DatabaseConfig): Database {
    val dataSource = constructDataSource(config)

    logger.debug("Running Flyway migrations...")
    runFlywayMigrations(dataSource)

    logger.debug("Establishing database connection...")
    return Database.connect(dataSource)
}

private fun runFlywayMigrations(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .schemas("public")
        .load()
        .migrate()
}

private fun constructDataSource(config: DatabaseConfig): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = config.url
        maximumPoolSize = config.maxPoolSize
        minimumIdle = config.minIdle
        username = config.username
        password = config.password
    }

    return HikariDataSource(config)
}
