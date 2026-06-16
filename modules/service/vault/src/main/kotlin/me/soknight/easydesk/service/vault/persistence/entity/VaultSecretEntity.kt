package me.soknight.easydesk.service.vault.persistence.entity

import kotlin.time.Instant
import me.soknight.easydesk.service.vault.domain.VaultSecret
import me.soknight.easydesk.service.vault.persistence.table.VaultSecretsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class VaultSecretEntity(id: EntityID<Long>) : LongEntity(id) {

    var createdAt      : Instant  by VaultSecretsTable.createdAt
    var description    : String?  by VaultSecretsTable.description
    var encryptedValue : String   by VaultSecretsTable.encryptedValue
    var name           : String   by VaultSecretsTable.name
    var updatedAt      : Instant  by VaultSecretsTable.updatedAt

    fun toDomain() = VaultSecret(
        createdAt = createdAt,
        description = description,
        encryptedValue = encryptedValue,
        id = id.value,
        name = name,
        updatedAt = updatedAt,
    )

    companion object : LongEntityClass<VaultSecretEntity>(VaultSecretsTable)

}
