package me.soknight.easydesk.service.vault.persistence.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

internal object VaultSecretsTable : LongIdTable("vault_secrets") {

    val createdAt = timestamp("created_at")
    val description = text("description").nullable()
    val encryptedValue = text("encrypted_value")
    val name = varchar("name", 64).uniqueIndex()
    val updatedAt = timestamp("updated_at")

}
