@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.channels.persistence.repository

import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.channels.data.domain.IdentityNote
import me.soknight.easydesk.service.channels.data.repository.IdentityNoteRepository
import me.soknight.easydesk.service.channels.persistence.entity.IdentityNoteEntity
import me.soknight.easydesk.service.channels.persistence.table.IdentityNotesTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Single
internal class DefaultIdentityNoteRepository : IdentityNoteRepository {

    override suspend fun create(identityId: Long, text: String, authorAgentId: Uuid): IdentityNote =
        suspendTransaction {
            val now = Clock.System.now()
            IdentityNoteEntity.new {
                this.authorAgentId = authorAgentId
                this.createdAt = now
                this.identityId = identityId
                this.text = text
                this.updatedAt = now
            }
        }.toDomain()

    override suspend fun findByIdentity(identityId: Long): List<IdentityNote> =
        suspendTransaction {
            IdentityNoteEntity
                .find { IdentityNotesTable.identityId eq identityId }
                .orderBy(IdentityNotesTable.createdAt to SortOrder.DESC)
                .map(IdentityNoteEntity::toDomain)
        }

    override suspend fun findById(id: Long): IdentityNote? =
        suspendTransaction { IdentityNoteEntity.findById(id) }?.toDomain()

    override suspend fun update(id: Long, text: String): IdentityNote? =
        suspendTransaction {
            val entity = IdentityNoteEntity.findById(id) ?: return@suspendTransaction null
            entity.text = text
            entity.updatedAt = Clock.System.now()
            entity
        }?.toDomain()

    override suspend fun delete(id: Long): Boolean =
        suspendTransaction {
            val entity = IdentityNoteEntity.findById(id) ?: return@suspendTransaction false
            entity.delete()
            true
        }

}
