@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.channels.persistence.entity

import me.soknight.easydesk.service.channels.data.domain.IdentityNote
import me.soknight.easydesk.service.channels.persistence.table.IdentityNotesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class IdentityNoteEntity(id: EntityID<Long>) : LongEntity(id) {

    var authorAgentId : Uuid by IdentityNotesTable.authorAgentId
    var createdAt     : Instant by IdentityNotesTable.createdAt
    var identityId    : Long by IdentityNotesTable.identityId
    var text          : String by IdentityNotesTable.text
    var updatedAt     : Instant by IdentityNotesTable.updatedAt

    fun toDomain() = IdentityNote(
        authorAgentId = authorAgentId,
        createdAt = createdAt,
        identifier = id.value,
        identityId = identityId,
        text = text,
        updatedAt = updatedAt,
    )

    companion object : LongEntityClass<IdentityNoteEntity>(IdentityNotesTable)

}
