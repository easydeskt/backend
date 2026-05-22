@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.repository

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.domain.TicketMessage
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageStats

/**
 * Persistence contract for [TicketMessage] management.
 *
 * All methods run inside a suspended transaction.
 */
interface TicketMessageRepository {

    /**
     * Creates and persists a new message within a ticket.
     *
     * Exactly one of [senderAgentId] / [senderIdentityId] must be non-null for
     * [ActorKind.AGENT] and [ActorKind.IDENTITY] respectively; both must be `null`
     * for [ActorKind.SYSTEM]. This invariant is enforced by the DB CHECK constraint.
     *
     * @param ticketId id of the ticket this message belongs to
     * @param nativeId platform-specific message id (used for deduplication)
     * @param senderKind role of the sender
     * @param senderAgentId id of the sending agent, or `null`
     * @param senderIdentityId id of the sending identity, or `null`
     * @param plainText text body, or `null` for media-only messages
     * @param inReplyToNativeId native id of the replied-to message, or `null`
     * @param platformTimestamp when the message was sent on the platform
     * @param attributes extensible platform-specific metadata; defaults to an empty object
     * @return the persisted [TicketMessage] with its generated [id][TicketMessage.identifier]
     */
    suspend fun create(
        ticketId: Long,
        nativeId: String,
        senderKind: ActorKind,
        senderAgentId: Uuid?,
        senderIdentityId: Long?,
        plainText: String?,
        inReplyToNativeId: String?,
        platformTimestamp: Instant,
        attributes: JsonObject = JsonObject(emptyMap()),
    ): TicketMessage

    /**
     * Returns the message with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): TicketMessage?

    /**
     * Returns the message for the given ticket and platform-native id, or `null` if not found.
     *
     * Used for deduplication when a platform delivers the same message twice.
     *
     * @param ticketId id of the ticket
     * @param nativeId platform-specific message id
     */
    suspend fun findByNativeId(ticketId: Long, nativeId: String): TicketMessage?

    /**
     * Returns all messages for the given ticket in ascending platform-timestamp order.
     */
    suspend fun findByTicket(ticketId: Long): List<TicketMessage>

    /**
     * Returns lightweight aggregated statistics for a ticket's messages.
     *
     * Computes last message timestamp, preview text, attachment count, and unread count in a
     * minimal set of queries — avoids materialising full [TicketMessage] objects.
     *
     * Unread count is the number of [ActorKind.IDENTITY] messages whose id is greater than the
     * read marker. When [readUpToMessageId] is `null` the last [ActorKind.AGENT] message id is
     * used as the marker; if no agent messages exist all identity messages are counted as unread.
     *
     * @param ticketId id of the ticket to compute statistics for
     * @param readUpToMessageId explicit read-marker, or `null` to derive it automatically
     */
    suspend fun getStats(ticketId: Long, readUpToMessageId: Long?): TicketMessageStats

}
