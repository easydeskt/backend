package me.soknight.easydesk.channel.vkontakte

import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.vkontakte.vk.VkBot
import me.soknight.easydesk.channel.vkontakte.vk.model.VkMessage

class VKontakteConversation(
    override val attributes: Attributes = emptyMap(),
    val bot: VkBot,
    override val channel: VKontakteChannel,
    val peerId: Long,
) : Conversation {

    override suspend fun send(message: Message, replyToNativeId: String?): Message =
        send(replyToNativeId) { copyFrom(message) }

    override suspend fun send(replyToNativeId: String?, block: MessageBuilder.() -> Unit): Message {
        val builder = VKontakteMessageBuilder().apply(block)
        val cmid = bot.apiClient.sendMessage(
            peerId = peerId,
            text = builder.plainText ?: "",
            replyTo = replyToNativeId?.toIntOrNull(),
        )
        val sentVkMessage = VkMessage(
            attachments = emptyList(),
            conversationMessageId = cmid,
            date = System.currentTimeMillis() / 1000,
            fromId = 0,
            fwdMessages = emptyList(),
            geo = null,
            id = 0,
            isOut = true,
            peerId = peerId,
            replyMessage = null,
            text = builder.plainText ?: "",
        )
        return VKontakteMessage(
            conversation = this,
            vkMessage = sentVkMessage,
            sender = ChannelActor.System,
            receiver = ChannelActor.Unknown,
            overrideAttributes = builder.builtAttributes,
        )
    }

}
