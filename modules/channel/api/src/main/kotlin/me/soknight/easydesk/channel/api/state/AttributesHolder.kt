package me.soknight.easydesk.channel.api.state

import me.soknight.easydesk.channel.api.dsl.Attributes

/**
 * An object that carries platform-specific [Attributes].
 *
 * Implemented by [Conversation][me.soknight.easydesk.channel.api.model.Conversation],
 * [Message][me.soknight.easydesk.channel.api.model.Message], and
 * [Attachment][me.soknight.easydesk.channel.api.model.Attachment] to store
 * metadata that doesn't map to a dedicated property.
 *
 * @see Attributes
 */
interface AttributesHolder {

    /** Platform-specific metadata associated with this object. */
    val attributes: Attributes

}