package me.soknight.easydesk.core.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * Runs [block] inside a suspended Exposed transaction on [Dispatchers.IO].
 *
 * @param database target database; uses the default Exposed connection when `null`
 * @param block transaction body — all Exposed DSL calls go here
 */
suspend fun <T> suspendTransaction(database: Database? = null, block: suspend JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction(db = database, statement = block)
    }
