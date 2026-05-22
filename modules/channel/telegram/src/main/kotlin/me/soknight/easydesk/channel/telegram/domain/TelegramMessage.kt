package me.soknight.easydesk.channel.telegram.domain

import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.types.MessageId
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.telegram.dsl.TelegramMessageBuilder

/**
 * Telegram-specific [Message] that wraps a received or sent Telegram message.
 *
 * @property conversation the [TelegramConversation] this message belongs to
 * @property messageId the Telegram message identifier
 */
class TelegramMessage(
    override val conversation: TelegramConversation,
    val messageId: MessageId,
    override val receiver: ChannelActor,
    override val sender: ChannelActor,
    override val attachments: Attachments = emptyList(),
    override val attributes: Attributes = emptyMap(),
    override val plainText: String? = null,
) : Message {

    override fun copy(block: MessageBuilder.() -> Unit): MessageBuilder =
        TelegramMessageBuilder().apply { copyFrom(this@TelegramMessage) }.apply(block)

    override suspend fun delete() {
        conversation.bot.deleteMessage(conversation.userChatId, messageId)
    }

    override suspend fun edit(block: MessageBuilder.() -> Unit): Message {
        val builder = TelegramMessageBuilder().apply(block)
        val edited = conversation.bot.editMessageText(
            chatId = conversation.userChatId,
            messageId = messageId,
            text = builder.plainText ?: "",
        )
        return TelegramMessage(
            conversation = conversation,
            sender = sender,
            receiver = receiver,
            messageId = edited.messageId,
            plainText = builder.plainText,
            attachments = builder.builtAttachments,
            attributes = builder.builtAttributes,
        )
    }

    override val nativeId: String get() = messageId.long.toString()

    override suspend fun reply(block: MessageBuilder.() -> Unit): Message =
        conversation.send(replyToNativeId = nativeId, block = block)

}
