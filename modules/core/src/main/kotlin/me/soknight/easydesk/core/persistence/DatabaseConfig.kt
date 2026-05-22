package me.soknight.easydesk.core.persistence

/**
 * Connection parameters for the application database.
 *
 * Typically constructed from the `easydesk.database` section of `application.yaml`
 * (backed by environment variables) and passed to [initDatabase].
 *
 * @param maxPoolSize HikariCP maximum pool size
 * @param minIdle HikariCP minimum number of idle connections kept alive
 * @param password database user password
 * @param url JDBC connection URL (e.g. `jdbc:postgresql://localhost:5432/easydesk`)
 * @param username database username
 * @see initDatabase
 */
data class DatabaseConfig(
    val maxPoolSize: Int,
    val minIdle: Int,
    val password: String,
    val url: String,
    val username: String,
)
