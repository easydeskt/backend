@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.table

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import kotlin.uuid.ExperimentalUuidApi

internal object TicketMessagesTable : LongIdTable("ticket_messages") {

    val attributes = jsonb<JsonObject>("attributes", Json)
    val createdAt = timestamp("created_at")
    val inReplyToNativeId = varchar("in_reply_to_native_id", 256).nullable()
    val nativeId = varchar("native_id", 256)
    val plainText = text("plain_text").nullable()
    val platformTimestamp = timestamp("platform_timestamp")
    val senderAgentId = uuid("sender_agent_id").nullable()
    val senderIdentityId = long("sender_identity_id").nullable()
    val senderKind = enumerationByName<ActorKind>("sender_kind", 16)
    val ticketId = long("ticket_id")

}
