package me.soknight.easydesk.channel.api.model

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.state.AttributesHolder
import me.soknight.easydesk.channel.api.state.ChannelScoped

/**
 * A message that has been sent to or received from a channel.
 *
 * Messages are immutable snapshots of platform state. To modify content,
 * use [copy] to obtain a [MessageBuilder], then [edit] or [reply] to push
 * changes back to the platform.
 *
 * Platform-specific metadata that doesn't map to a dedicated property can be
 * stored in [attributes][me.soknight.easydesk.channel.api.state.AttributesHolder.attributes].
 *
 * @see Conversation
 * @see MessageBuilder
 * @see Attachment
 */
interface Message : AttributesHolder, ChannelScoped {

    /** The channel (connection) this message belongs to. Derived from [conversation]. */
    override val channel: Channel
        get() = conversation.channel

    /** The conversation this message belongs to. */
    val conversation: Conversation

    /** Platform-native identifier of this message. */
    val nativeId: String

    /** The actor who sent this message. */
    val sender: ChannelActor

    /** The actor who received this message. */
    val receiver: ChannelActor

    /** Attachments associated with this message. Defaults to an empty list. */
    val attachments: Attachments
        get() = emptyList()

    /** Plain-text content of the message, or `null` if the message has no text. */
    val plainText: String?
        get() = null

    /** Creates a mutable [MessageBuilder] pre-populated with this message's content and applies [block] to it. */
    fun copy(block: MessageBuilder.() -> Unit): MessageBuilder

    /**
     * Deletes this message from the channel.
     *
     * @throws UnsupportedOperationException if the platform does not support
     *   message deletion (check [ChannelBrand.isSupported] with [ChannelBrand.Feature.MESSAGE_DELETE])
     */
    @Throws(UnsupportedOperationException::class)
    suspend fun delete()

    /**
     * Edits this message by applying [block] to a mutable copy and pushing the result to the platform.
     *
     * @param block builder block to modify the message content
     * @return the updated message as it now exists on the platform
     * @throws UnsupportedOperationException if the platform does not support
     *   message editing (check [ChannelBrand.isSupported] with [ChannelBrand.Feature.MESSAGE_EDIT])
     */
    @Throws(UnsupportedOperationException::class)
    suspend fun edit(block: MessageBuilder.() -> Unit): Message

    /**
     * Sends a reply to this message by composing a new one via [block].
     *
     * @param block builder block to compose the reply content
     * @return the sent reply as it exists on the platform
     * @throws UnsupportedOperationException if the platform cannot reply
     */
    @Throws(UnsupportedOperationException::class)
    suspend fun reply(block: MessageBuilder.() -> Unit): Message

}