package me.soknight.easydesk.supervisor.api.event

import me.soknight.easydesk.core.event.Event
import me.soknight.easydesk.supervisor.api.model.TicketMessage

/**
 * Events that describe changes to messages within a ticket thread.
 *
 * Published by `service:tickets` after message persistence;
 * consumed by supervisor surfaces to mirror messages to forum topics.
 *
 * @see me.soknight.easydesk.supervisor.api.model.TicketMessage
 */
sealed interface TicketMessageEvent : Event {

    /** A message was deleted. Carries minimal context since the full record is gone. */
    data class Deleted(
        val messageId: Long,
        val nativeId: String,
        val ticketId: Long,
    ) : TicketMessageEvent

    /** An existing message was edited. */
    data class Edited(val message: TicketMessage) : TicketMessageEvent

    /** A new message was recorded in the ticket thread. */
    data class Recorded(
        val conversationId: Long,
        val message: TicketMessage,
    ) : TicketMessageEvent

}
