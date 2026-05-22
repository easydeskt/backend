package me.soknight.easydesk.service.tickets.data.domain

import me.soknight.easydesk.service.storage.data.domain.Attachment

/**
 * Association between a [TicketMessage] and a stored [Attachment].
 *
 * @param messageId id of the owning message
 * @param attachment the stored file
 *
 * @see TicketMessage
 * @see Attachment
 */
data class TicketMessageAttachment(
    val messageId: Long,
    val attachment: Attachment,
)
