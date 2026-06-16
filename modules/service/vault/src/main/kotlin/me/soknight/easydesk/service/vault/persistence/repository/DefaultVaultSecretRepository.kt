package me.soknight.easydesk.service.vault.persistence.repository

import java.sql.SQLException
import kotlin.time.Clock
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.vault.domain.VaultSecret
import me.soknight.easydesk.service.vault.persistence.entity.VaultSecretEntity
import me.soknight.easydesk.service.vault.persistence.table.VaultSecretsTable
import me.soknight.easydesk.service.vault.repository.VaultSecretRepository
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single

@Single
internal class DefaultVaultSecretRepository : VaultSecretRepository {

    override suspend fun create(name: String, description: String?, encryptedValue: String): VaultSecret? =
        try {
            suspendTransaction {
                val now = Clock.System.now()
                VaultSecretEntity.new {
                    this.createdAt = now
                    this.description = description
                    this.encryptedValue = encryptedValue
                    this.name = name
                    this.updatedAt = now
                }.toDomain()
            }
        } catch (e: Exception) {
            // unique constraint violation (PG SQLSTATE 23505) → name already exists
            val sqlEx = generateSequence(e as Throwable) { it.cause }.filterIsInstance<SQLException>().firstOrNull()
            if (sqlEx?.sqlState == "23505") null else throw e
        }

    override suspend fun delete(id: Long): Boolean =
        suspendTransaction {
            val entity = VaultSecretEntity.findById(id) ?: return@suspendTransaction false
            entity.delete()
            true
        }

    override suspend fun findAll(): List<VaultSecret> =
        suspendTransaction {
            VaultSecretEntity.all()
                .orderBy(VaultSecretsTable.name to SortOrder.ASC)
                .map(VaultSecretEntity::toDomain)
        }

    override suspend fun findById(id: Long): VaultSecret? =
        suspendTransaction { VaultSecretEntity.findById(id) }?.toDomain()

    override suspend fun findByName(name: String): VaultSecret? =
        suspendTransaction {
            VaultSecretEntity.find { VaultSecretsTable.name eq name }.singleOrNull()
        }?.toDomain()

    override suspend fun update(id: Long, description: String?, encryptedValue: String?): VaultSecret? =
        suspendTransaction {
            val entity = VaultSecretEntity.findById(id) ?: return@suspendTransaction null
            description?.let { entity.description = it.ifBlank { null } }
            encryptedValue?.let { entity.encryptedValue = it }
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

}
