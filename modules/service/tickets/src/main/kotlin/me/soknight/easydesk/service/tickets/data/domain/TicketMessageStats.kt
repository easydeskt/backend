package me.soknight.easydesk.service.tickets.data.domain

import kotlin.time.Instant

/**
 * Lightweight aggregated statistics for messages of a single ticket.
 *
 * @param attachmentCount total number of attachments across all messages in the ticket
 * @param lastMessageAt platform timestamp of the most recent message, or `null` if none exist
 * @param previewText plain-text body of the most recent message, or `null` if the message is media-only
 * @param unreadCount number of unread IDENTITY messages
 */
data class TicketMessageStats(
    val attachmentCount: Int,
    val lastMessageAt: Instant?,
    val previewText: String?,
    val unreadCount: Int,
)
