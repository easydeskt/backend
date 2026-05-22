package me.soknight.easydesk.channel.vkontakte

import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.vkontakte.vk.model.VkMessage

class VKontakteMessage(
    override val attachments: Attachments = emptyList(),
    override val attributes: Attributes = emptyMap(),
    override val conversation: VKontakteConversation,
    override val receiver: ChannelActor,
    override val sender: ChannelActor,
    val vkMessage: VkMessage,
    private val overrideAttributes: Attributes = emptyMap(),
) : Message {

    override val nativeId: String get() = vkMessage.conversationMessageId.toString()

    override val plainText: String? get() = vkMessage.text.takeIf { it.isNotEmpty() }

    override fun copy(block: MessageBuilder.() -> Unit): MessageBuilder =
        VKontakteMessageBuilder().apply { copyFrom(this@VKontakteMessage) }.apply(block)

    override suspend fun delete() {
        conversation.bot.apiClient.deleteMessages(
            peerId = vkMessage.peerId,
            conversationMessageIds = listOf(vkMessage.conversationMessageId),
        )
    }

    override suspend fun edit(block: MessageBuilder.() -> Unit): Message {
        val builder = VKontakteMessageBuilder().apply(block)
        conversation.bot.apiClient.editMessage(
            peerId = vkMessage.peerId,
            conversationMessageId = vkMessage.conversationMessageId,
            text = builder.plainText ?: "",
        )
        return VKontakteMessage(
            conversation = conversation,
            vkMessage = vkMessage.copy(text = builder.plainText ?: ""),
            sender = sender,
            receiver = receiver,
            attachments = builder.builtAttachments,
            attributes = builder.builtAttributes,
        )
    }

    override suspend fun reply(block: MessageBuilder.() -> Unit): Message =
        conversation.send(replyToNativeId = nativeId, block = block)

}
