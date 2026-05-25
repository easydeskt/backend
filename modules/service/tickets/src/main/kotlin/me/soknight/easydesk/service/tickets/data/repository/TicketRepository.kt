@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.repository

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.service.tickets.data.domain.Ticket
import me.soknight.easydesk.supervisor.api.SupervisorBrand
import me.soknight.easydesk.supervisor.api.model.Ticket as SupervisorTicket

/**
 * Persistence contract for [Ticket] management.
 *
 * All methods run inside a suspended transaction.
 */
interface TicketRepository {

    /**
     * Assigns the ticket to an agent or removes the current assignment.
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to update
     * @param agentId id of the new assignee, or `null` to unassign
     */
    suspend fun assign(id: Long, agentId: Uuid?): Ticket?

    /**
     * Returns the average time in minutes from ticket creation to the first message where
     * `sender_kind = AGENT`, computed only over tickets that have at least one such message.
     * Returns `null` when no qualifying tickets exist.
     */
    suspend fun avgFirstResponseTimeMinutes(): Double?

    /**
     * Returns the average time in minutes from ticket creation to the first agent message
     * sent by [agentId], computed only over tickets assigned to that agent that have at least
     * one such message. Returns `null` when no qualifying tickets exist.
     */
    suspend fun avgFirstResponseTimeMinutes(agentId: Uuid): Double?

    /**
     * Returns the count of tickets assigned to [agentId] that reached
     * [SupervisorTicket.Status.RESOLVED] today (server UTC date).
     */
    suspend fun resolvedTodayCount(agentId: Uuid): Int

    /**
     * Transitions the ticket to [SupervisorTicket.Status.CLOSED].
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to close
     */
    suspend fun close(id: Long): Ticket?

    /**
     * Returns the total count of tickets whose status is any of [statuses].
     *
     * @param statuses one or more statuses to match; passing none returns 0
     */
    suspend fun countByStatuses(vararg statuses: SupervisorTicket.Status): Long

    /**
     * Creates and persists a new ticket in [SupervisorTicket.Status.OPEN] state.
     *
     * @param conversationId id of the conversation that originated the ticket
     * @param priority initial urgency level; defaults to [SupervisorTicket.Priority.MEDIUM]
     * @return the persisted [Ticket] with its generated [id][Ticket.identifier]
     */
    suspend fun create(
        conversationId: Long,
        priority: SupervisorTicket.Priority = SupervisorTicket.Priority.MEDIUM,
    ): Ticket

    /**
     * Returns all tickets in descending creation order.
     */
    suspend fun findAll(): List<Ticket>

    /**
     * Returns all tickets assigned to the given agent.
     */
    suspend fun findByAssignedAgent(agentId: Uuid): List<Ticket>

    /**
     * Returns all tickets belonging to the given conversation.
     */
    suspend fun findByConversation(conversationId: Long): List<Ticket>

    /**
     * Returns the ticket with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): Ticket?

    /**
     * Returns all tickets with the given [status].
     */
    suspend fun findByStatus(status: SupervisorTicket.Status): List<Ticket>

    /**
     * Returns the most recently created [Ticket] for [conversationId] whose
     * status is [SupervisorTicket.Status.OPEN] or [SupervisorTicket.Status.IN_PROGRESS],
     * or `null` if none exists.
     */
    suspend fun findOpenByConversation(conversationId: Long): Ticket?

    /**
     * Returns the supervisor platform native id bound to the given ticket, or `null` if not bound.
     *
     * @param ticketId id of the ticket
     * @param brand supervisor platform brand
     */
    suspend fun findSupervisorBinding(ticketId: Long, brand: SupervisorBrand): String?

    /**
     * Reverse lookup: given a platform topic native id, returns the bound ticket id, or `null` if not found.
     *
     * @param brand supervisor platform brand
     * @param nativeId platform-specific topic id (e.g. Telegram `message_thread_id` as string)
     */
    suspend fun findTicketBySupervisorBinding(brand: SupervisorBrand, nativeId: String): Long?

    /**
     * Removes the current agent assignment from the ticket (sets [Ticket.assignedAgentId] to `null`).
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to free
     */
    suspend fun free(id: Long): Ticket?

    /**
     * Binds a ticket to a supervisor platform topic.
     *
     * The pair `(brand, nativeId)` must be unique across all tickets; duplicate bindings
     * will throw a database constraint violation.
     *
     * @param ticketId id of the ticket to bind
     * @param brand supervisor platform brand
     * @param nativeId platform-specific topic id (e.g. Telegram `message_thread_id` as string)
     */
    suspend fun linkSupervisor(ticketId: Long, brand: SupervisorBrand, nativeId: String)

    /**
     * Merges [sourceId] into [targetId] following domain §9 rules:
     * - all `ticket_messages` of source are re-parented to target
     * - tags from source not already on target are added to target (preserving sort order)
     * - source status becomes [SupervisorTicket.Status.MERGED] with [Ticket.mergedIntoTicketId] = targetId
     * - target status becomes [SupervisorTicket.Status.OPEN] and is assigned to [byAgentId]
     *
     * Both tickets must belong to the same conversation. Returns the updated `(source, target)` pair,
     * or `null` if either ticket does not exist or they are in different conversations.
     *
     * @param sourceId id of the ticket to be merged (becomes MERGED)
     * @param targetId id of the ticket that absorbs the source
     * @param byAgentId id of the agent initiating the merge
     */
    suspend fun merge(sourceId: Long, targetId: Long, byAgentId: Uuid): Pair<Ticket, Ticket>?

    /**
     * Transitions the ticket back to [SupervisorTicket.Status.OPEN].
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to reopen
     * @param byAgentId id of the agent performing the action (for audit purposes)
     */
    suspend fun reopen(id: Long, byAgentId: Uuid): Ticket?

    /**
     * Transitions the ticket to [SupervisorTicket.Status.RESOLVED].
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to resolve
     */
    suspend fun resolve(id: Long): Ticket?

    /**
     * Merges [patch] into the ticket's [Ticket.attributes] object when [replace] is `false`,
     * or replaces it entirely when [replace] is `true`. In both cases, keys whose value is
     * `JsonNull` are removed from the resulting object.
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to update
     * @param patch attributes to apply
     * @param replace when `true` the existing attributes are discarded; when `false` they are merged
     */
    suspend fun updateAttributes(id: Long, patch: JsonObject, replace: Boolean): Ticket?

    /**
     * Changes the priority of the ticket.
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to update
     * @param priority new urgency level
     */
    suspend fun updatePriority(id: Long, priority: SupervisorTicket.Priority): Ticket?

    /**
     * Advances the read marker to [messageId], meaning all messages up to and including
     * that id are considered read by agents.
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket
     * @param messageId id of the last read message
     */
    suspend fun updateReadMarker(id: Long, messageId: Long): Ticket?

    /**
     * Transitions the ticket to a new status.
     * Returns the updated [Ticket], or `null` if no ticket with [id] exists.
     *
     * @param id id of the ticket to update
     * @param status new lifecycle state
     * @param mergedIntoTicketId required when [status] is [SupervisorTicket.Status.MERGED]; `null` otherwise
     */
    suspend fun updateStatus(
        id: Long,
        status: SupervisorTicket.Status,
        mergedIntoTicketId: Long? = null,
    ): Ticket?

}
