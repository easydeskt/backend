package me.soknight.easydesk.channel.email.domain

import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.email.dsl.EmailMessageBuilder

/**
 * Email-specific [Message] wrapping a received or sent email.
 *
 * Email does not support editing or deletion — both operations throw [UnsupportedOperationException].
 * Replying delegates to [EmailConversation.send] with [nativeId] as `replyToNativeId`,
 * which sets the correct `In-Reply-To` header for threading.
 */
class EmailMessage(
    override val conversation: EmailConversation,
    override val nativeId: String,
    override val sender: ChannelActor,
    override val receiver: ChannelActor,
    override val plainText: String?,
    override val attachments: Attachments = emptyList(),
    override val attributes: Attributes = emptyMap(),
) : Message {

    override fun copy(block: MessageBuilder.() -> Unit): MessageBuilder =
        EmailMessageBuilder().apply { copyFrom(this@EmailMessage) }.apply(block)

    override suspend fun delete(): Unit =
        throw UnsupportedOperationException("Email does not support message deletion")

    override suspend fun edit(block: MessageBuilder.() -> Unit): Message =
        throw UnsupportedOperationException("Email does not support message editing")

    override suspend fun reply(block: MessageBuilder.() -> Unit): Message =
        conversation.send(replyToNativeId = nativeId, block = block)

}
