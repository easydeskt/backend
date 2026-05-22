package me.soknight.easydesk.channel.email.event

import kotlin.time.Instant
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.event.MessageEvent
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.email.domain.EmailConversation

/**
 * Emitted when an email is received in a configured mailbox.
 *
 * Implements [MessageEvent.Received] so that [me.soknight.easydesk.service.tickets.data.event.MessageEventHandler]
 * picks it up and routes it into the ticket pipeline.
 */
data class EmailMessageReceived(
    override val conversation: EmailConversation,
    override val message: Message,
    override val timestamp: Instant,
) : MessageEvent.Received {

    override val channel: Channel get() = conversation.channel

}
