@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.entity

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.domain.TicketMessage
import me.soknight.easydesk.service.tickets.persistence.table.TicketMessagesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

internal class TicketMessageEntity(id: EntityID<Long>) : LongEntity(id) {

    var attributes          : JsonObject by TicketMessagesTable.attributes
    var createdAt           : Instant by TicketMessagesTable.createdAt
    var inReplyToNativeId   : String? by TicketMessagesTable.inReplyToNativeId
    var nativeId            : String by TicketMessagesTable.nativeId
    var plainText           : String? by TicketMessagesTable.plainText
    var platformTimestamp   : Instant by TicketMessagesTable.platformTimestamp
    var senderAgentId       : Uuid? by TicketMessagesTable.senderAgentId
    var senderIdentityId    : Long? by TicketMessagesTable.senderIdentityId
    var senderKind          : ActorKind by TicketMessagesTable.senderKind
    var ticketId            : Long by TicketMessagesTable.ticketId

    fun toDomain(
        conversation: Conversation,
        attachments: Attachments,
        senderIdentity: ChannelIdentity?,
    ) = TicketMessage(
        attachments = attachments,
        conversation = conversation,
        createdAt = createdAt,
        identifier = id.value,
        inReplyToNativeId = inReplyToNativeId,
        nativeId = nativeId,
        plainText = plainText,
        platformTimestamp = platformTimestamp,
        rawAttributes = attributes,
        senderAgentId = senderAgentId,
        senderIdentityId = senderIdentityId,
        senderIdentity = senderIdentity,
        senderKind = senderKind,
        ticketId = ticketId,
    )

    companion object : LongEntityClass<TicketMessageEntity>(TicketMessagesTable)

}
