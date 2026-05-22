package me.soknight.easydesk.service.channels.registry

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message

/**
 * Stub [Conversation] returned when the live conversation is not in [ConversationRegistry].
 * Read access (channel/attributes) returns inert defaults; any `send(...)` call throws.
 *
 * @param channel the [Channel] that owns this conversation context, used for read-only access
 */
class StaleConversation(override val channel: Channel) : Conversation {

    override val attributes: Attributes = emptyMap()

    override suspend fun send(
        message: Message,
        replyToNativeId: String?,
    ): Message =
        error("Cannot send through a stale conversation (no live channel:api Conversation cached)")

    override suspend fun send(
        replyToNativeId: String?,
        block: MessageBuilder.() -> Unit,
    ): Message =
        error("Cannot send through a stale conversation (no live channel:api Conversation cached)")

}
