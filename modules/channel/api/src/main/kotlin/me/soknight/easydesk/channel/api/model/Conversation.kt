package me.soknight.easydesk.channel.api.model

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.state.AttributesHolder
import me.soknight.easydesk.channel.api.state.ChannelScoped

/**
 * A per-user dialog within a [Channel].
 *
 * Represents an isolated message stream between the helpdesk system and
 * a specific user on a messaging platform. Each conversation belongs to
 * exactly one [Channel] (connection).
 *
 * Use [send] to dispatch outgoing messages to the platform.
 *
 * @see Channel
 * @see Message
 */
interface Conversation : AttributesHolder, ChannelScoped {

    /**
     * Sends a pre-built [message], optionally as a reply.
     *
     * @param message the message to send
     * @param replyToNativeId platform-native ID of the message to reply to, or `null`
     * @return the sent message as it exists on the platform
     */
    suspend fun send(
        message: Message,
        replyToNativeId: String? = null,
    ): Message

    /**
     * Composes a new message via [block] and sends it, optionally as a reply.
     *
     * @param replyToNativeId platform-native ID of the message to reply to, or `null`
     * @param block builder block applied to a fresh [MessageBuilder]
     * @return the sent message as it exists on the platform
     */
    suspend fun send(
        replyToNativeId: String? = null,
        block: MessageBuilder.() -> Unit,
    ): Message

}
