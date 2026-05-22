package me.soknight.easydesk.channel.telegram.event

import kotlin.time.Instant
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.event.MessageEvent
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.telegram.domain.TelegramConversation

/**
 * Emitted when a Telegram user sends a new message in a private chat.
 *
 * Implements [MessageEvent.Received] so that [me.soknight.easydesk.service.tickets.event.MessageEventHandler]
 * can pick it up and create or continue a ticket.
 *
 * @property conversation the [TelegramConversation] the message arrived in
 * @property message the received [Message]
 * @property timestamp when the message was received, as reported by Telegram
 */
data class TelegramMessageReceived(
    override val conversation: TelegramConversation,
    override val message: Message,
    override val timestamp: Instant,
) : MessageEvent.Received {

    override val channel: Channel get() = conversation.channel

}
