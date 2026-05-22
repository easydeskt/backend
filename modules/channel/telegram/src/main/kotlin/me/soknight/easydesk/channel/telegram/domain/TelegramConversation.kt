package me.soknight.easydesk.channel.telegram.domain

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.ReplyParameters
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.telegram.dsl.TelegramMessageBuilder

/**
 * Telegram-specific [Conversation] that sends messages to a user's private chat.
 *
 * @property bot the [TelegramBot] instance used to dispatch API calls
 * @property userChatId the Telegram chat identifier for the target user
 */
class TelegramConversation(
    override val attributes: Attributes,
    val bot: TelegramBot,
    override val channel: Channel,
    val userChatId: ChatIdentifier,
) : Conversation {

    override suspend fun send(message: Message, replyToNativeId: String?): Message =
        send(replyToNativeId = replyToNativeId) { copyFrom(message) }

    override suspend fun send(replyToNativeId: String?, block: MessageBuilder.() -> Unit): Message {
        val builder = TelegramMessageBuilder().apply(block)
        val replyParams = replyToNativeId?.let { ReplyParameters(userChatId, MessageId(it.toLong())) }
        val sent = bot.sendMessage(
            chatId = userChatId,
            text = builder.plainText ?: "",
            replyParameters = replyParams,
        )
        return TelegramMessage(
            conversation = this,
            sender = ChannelActor.System,
            receiver = ChannelActor.Unknown,
            messageId = sent.messageId,
            plainText = builder.plainText,
            attachments = builder.builtAttachments,
            attributes = builder.builtAttributes,
        )
    }

}
