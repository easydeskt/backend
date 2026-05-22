package me.soknight.easydesk.service.tickets.persistence.entity

import me.soknight.easydesk.service.tickets.data.domain.TicketTag
import me.soknight.easydesk.service.tickets.persistence.table.TicketTagsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import kotlin.time.Instant

internal class TicketTagEntity(id: EntityID<Long>) : LongEntity(id) {

    var color       : Int? by TicketTagsTable.color
    var createdAt   : Instant by TicketTagsTable.createdAt
    var name        : String by TicketTagsTable.name

    fun toDomain() = TicketTag(
        color = color,
        createdAt = createdAt,
        identifier = id.value,
        humanName = name,
    )

    companion object : LongEntityClass<TicketTagEntity>(TicketTagsTable)

}
