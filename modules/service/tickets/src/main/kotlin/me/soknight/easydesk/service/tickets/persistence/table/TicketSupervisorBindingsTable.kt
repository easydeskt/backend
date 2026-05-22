package me.soknight.easydesk.service.tickets.persistence.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object TicketSupervisorBindingsTable : Table("ticket_supervisor_bindings") {

    val createdAt = timestamp("created_at")
    val nativeId = varchar("native_id", 256)
    val supervisorBrand = varchar("supervisor_brand", 16)
    val ticketId = long("ticket_id")

    override val primaryKey = PrimaryKey(ticketId, supervisorBrand)

}
