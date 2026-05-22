package me.soknight.easydesk.channel.telegram.dsl

import me.soknight.easydesk.channel.api.dsl.Attachments
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.dsl.MutableAttachments
import me.soknight.easydesk.channel.api.dsl.MutableAttributes
import me.soknight.easydesk.channel.api.model.Message

/**
 * Telegram-specific [MessageBuilder] implementation.
 *
 * Collects plain text, attachments, and attributes during a builder block,
 * then exposes the built values for use by [me.soknight.easydesk.channel.telegram.domain.TelegramConversation].
 *
 * @see me.soknight.easydesk.channel.telegram.domain.TelegramConversation.send
 * @see me.soknight.easydesk.channel.telegram.domain.TelegramMessage.edit
 */
class TelegramMessageBuilder : MessageBuilder {

    private val _attachments: MutableAttachments = mutableListOf()
    private val _attributes: MutableAttributes = mutableMapOf()

    override fun attachments(block: MutableAttachments.() -> Unit) { _attachments.block() }

    override fun attributes(block: MutableAttributes.() -> Unit) { _attributes.block() }

    val builtAttachments: Attachments get() = _attachments.toList()

    val builtAttributes: Attributes get() = _attributes.toMap()

    override fun copyFrom(message: Message) {
        plainText = message.plainText
        _attachments.addAll(message.attachments)
        _attributes.putAll(message.attributes)
    }

    override var plainText: String? = null

}
