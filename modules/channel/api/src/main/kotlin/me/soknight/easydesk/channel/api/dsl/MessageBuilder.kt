package me.soknight.easydesk.channel.api.dsl

import me.soknight.easydesk.channel.api.model.Message

/**
 * DSL builder for composing or modifying message content.
 *
 * Obtained via [Message.copy], [Message.reply], or [Conversation.send][me.soknight.easydesk.channel.api.model.Conversation.send].
 *
 * ```kotlin
 * channel.send {
 *     plainText = "Here is the report"
 *     attachments { add(pdfAttachment) }
 *     attributes { this["priority"] = "high" }
 * }
 * ```
 *
 * @see Message
 * @see me.soknight.easydesk.channel.api.model.Conversation.send
 */
interface MessageBuilder {

    /** Mutates the attachment list using the given [block]. */
    fun attachments(block: MutableAttachments.() -> Unit)

    /** Mutates the attributes using the given [block]. */
    fun attributes(block: MutableAttributes.() -> Unit)

    /** Mutable plain-text content. */
    var plainText: String?

    /** Replaces this builder's content with values from [message]. */
    fun copyFrom(message: Message)

}