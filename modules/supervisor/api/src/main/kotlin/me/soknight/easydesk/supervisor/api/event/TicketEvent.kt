@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.event

import me.soknight.easydesk.core.event.Event
import me.soknight.easydesk.supervisor.api.model.Ticket
import me.soknight.easydesk.supervisor.api.model.TicketTag
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Events that describe lifecycle changes to a [Ticket].
 *
 * Published by `service:tickets` after a successful transaction;
 * consumed by supervisor surfaces to mirror state changes to the supergroup topics.
 *
 * @see me.soknight.easydesk.supervisor.api.model.Ticket
 */
sealed interface TicketEvent : Event {

    /** An agent was assigned to the ticket. */
    data class Assigned(
        val previousAgentId: Uuid?,
        val ticket: Ticket,
    ) : TicketEvent

    /** A ticket was created from an incoming conversation. */
    data class Created(val ticket: Ticket) : TicketEvent

    /** Two tickets were merged; [source] is now in [Ticket.Status.MERGED] state. */
    data class Merged(
        val source: Ticket,
        val target: Ticket,
    ) : TicketEvent

    /** The ticket's priority changed. */
    data class PriorityChanged(
        val previous: Ticket.Priority,
        val ticket: Ticket,
    ) : TicketEvent

    /** The assigned agent was released from the ticket. */
    data class Released(
        val previousAgentId: Uuid,
        val ticket: Ticket,
    ) : TicketEvent

    /** The ticket's status changed. */
    data class StatusChanged(
        val previous: Ticket.Status,
        val ticket: Ticket,
    ) : TicketEvent

    /** A tag was applied to the ticket. */
    data class Tagged(
        val byAgentId: Uuid,
        val tag: TicketTag,
        val ticketId: Long,
    ) : TicketEvent

    /** A tag was removed from the ticket. */
    data class Untagged(
        val byAgentId: Uuid,
        val tag: TicketTag,
        val ticketId: Long,
    ) : TicketEvent

}
