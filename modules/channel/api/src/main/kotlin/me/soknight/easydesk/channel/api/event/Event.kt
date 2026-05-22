package me.soknight.easydesk.channel.api.event

import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.state.ChannelScoped
import me.soknight.easydesk.core.event.Event
import kotlin.time.Instant

/**
 * Base type for all events occurring within a [Conversation].
 *
 * Represents inbound activity from the messaging platform
 * (new messages, edits, deletions, etc.).
 *
 * @property conversation the conversation this event originated from
 * @property timestamp when the event occurred, as reported by the platform
 * @see MessageEvent
 */
sealed interface ChannelEvent : Event, ChannelScoped {

    val conversation: Conversation

    val timestamp: Instant

}
