@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.persistence.repository

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.core.persistence.suspendTransaction
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.channels.registry.ChannelRegistry
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.channels.registry.StaleConversation
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.domain.TicketMessage
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageStats
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.service.tickets.persistence.entity.TicketMessageEntity
import me.soknight.easydesk.service.tickets.persistence.table.TicketMessagesTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single

@Single
internal class DefaultTicketMessageRepository(
    private val channelIdentityRepository: ChannelIdentityRepository,
    private val channelRegistry: ChannelRegistry,
    private val conversationRegistry: ConversationRegistry,
    private val conversationRepository: ConversationRepository,
    private val ticketMessageAttachmentRepository: TicketMessageAttachmentRepository,
    private val ticketRepository: TicketRepository,
) : TicketMessageRepository {

    private suspend fun TicketMessageEntity.toFullDomain(): TicketMessage {
        val ticket = ticketRepository.findById(ticketId) ?: error("ticket $ticketId missing")
        val serviceConv = conversationRepository.findById(ticket.conversationId)
            ?: error("conversation ${ticket.conversationId} missing")
        val conv = conversationRegistry.getOrNull(serviceConv.id)
            ?: StaleConversation(
                channelRegistry.getOrNull(serviceConv.channelId)
                    ?: error("no live Channel resolved for service channel id ${serviceConv.channelId}"),
            )
        val identity = senderIdentityId?.let { channelIdentityRepository.findById(it) }
        val messageAttachments = ticketMessageAttachmentRepository.findByMessage(id.value)
        return toDomain(conv, messageAttachments, identity)
    }

    override suspend fun create(
        ticketId: Long,
        nativeId: String,
        senderKind: ActorKind,
        senderAgentId: Uuid?,
        senderIdentityId: Long?,
        plainText: String?,
        inReplyToNativeId: String?,
        platformTimestamp: Instant,
        attributes: JsonObject,
    ): TicketMessage =
        // toFullDomain() runs after the write transaction commits; parent ticket always pre-exists
        suspendTransaction {
            TicketMessageEntity.new {
                this.attributes = attributes
                this.createdAt = Clock.System.now()
                this.inReplyToNativeId = inReplyToNativeId
                this.nativeId = nativeId
                this.plainText = plainText
                this.platformTimestamp = platformTimestamp
                this.senderAgentId = senderAgentId
                this.senderIdentityId = senderIdentityId
                this.senderKind = senderKind
                this.ticketId = ticketId
            }
        }.toFullDomain()

    override suspend fun findById(id: Long): TicketMessage? =
        suspendTransaction { TicketMessageEntity.findById(id) }?.toFullDomain()

    override suspend fun findByNativeId(ticketId: Long, nativeId: String): TicketMessage? =
        suspendTransaction {
            with(TicketMessagesTable) {
                TicketMessageEntity
                    .find { (this.ticketId eq ticketId) and (this.nativeId eq nativeId) }
                    .singleOrNull()
            }
        }?.toFullDomain()

    override suspend fun getStats(ticketId: Long, readUpToMessageId: Long?): TicketMessageStats {
        val t = TicketMessagesTable

        val rows = suspendTransaction {
            t.selectAll()
                .where { t.ticketId eq ticketId }
                .orderBy(t.id to SortOrder.ASC)
                .toList()
        }

        val messageIds = rows.map { it[t.id].value }
        val attachmentCount = ticketMessageAttachmentRepository.countByMessageIds(messageIds)

        val lastByTimestamp = rows.maxByOrNull { it[t.platformTimestamp] }

        val marker = readUpToMessageId ?: rows
            .lastOrNull { it[t.senderKind] == ActorKind.AGENT }
            ?.get(t.id)
            ?.value

        val unreadCount = rows
            .filter { it[t.senderKind] == ActorKind.IDENTITY }
            .count { marker == null || it[t.id].value > marker }

        return TicketMessageStats(
            attachmentCount = attachmentCount,
            lastMessageAt = lastByTimestamp?.get(t.platformTimestamp),
            previewText = lastByTimestamp?.get(t.plainText),
            unreadCount = unreadCount,
        )
    }

    override suspend fun findByTicket(ticketId: Long): List<TicketMessage> {
        val entities = suspendTransaction {
            with(TicketMessagesTable) {
                TicketMessageEntity
                    .find { this.ticketId eq ticketId }
                    .orderBy(platformTimestamp to SortOrder.ASC)
                    .toList()
            }
        }
        if (entities.isEmpty()) return emptyList()

        // resolve ticket + conversation once for all messages in this ticket
        val ticket = ticketRepository.findById(ticketId) ?: error("ticket $ticketId missing")
        val serviceConv = conversationRepository.findById(ticket.conversationId)
            ?: error("conversation ${ticket.conversationId} missing")
        val conv = conversationRegistry.getOrNull(serviceConv.id)
            ?: StaleConversation(
                channelRegistry.getOrNull(serviceConv.channelId)
                    ?: error("no live Channel resolved for service channel id ${serviceConv.channelId}"),
            )

        return entities.map { entity ->
            val identity = entity.senderIdentityId?.let { channelIdentityRepository.findById(it) }
            val messageAttachments = ticketMessageAttachmentRepository.findByMessage(entity.id.value)
            entity.toDomain(conv, messageAttachments, identity)
        }
    }

}
