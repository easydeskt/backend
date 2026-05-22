package me.soknight.easydesk.channel.api.dsl

import me.soknight.easydesk.channel.api.model.Attachment

/** Immutable list of [Attachment]s associated with a message. */
typealias Attachments = List<Attachment>

/** Mutable list of [Attachment]s, used in [MessageBuilder.attachments] blocks. */
typealias MutableAttachments = MutableList<Attachment>
