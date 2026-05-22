@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Read-only view of a ticket message for supervisor surfaces.
 */
interface TicketMessage {

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** Native id of the message being replied to, or `null` for top-level messages. */
    val inReplyToNativeId: String?

    /** Platform-specific message id used for deduplication and reply threading. */
    val nativeId: String

    /** Text body, or `null` for media-only messages. */
    val plainText: String?

    /** When the message was sent on the platform. */
    val platformTimestamp: Instant

    /** Id of the sending agent; non-null iff [senderKind] is [ActorKind.AGENT]. */
    val senderAgentId: Uuid?

    /** Id of the sending identity; non-null iff [senderKind] is [ActorKind.IDENTITY]. */
    val senderIdentityId: Long?

    /** Role of the party who sent this message. */
    val senderKind: ActorKind

    /** Id of the ticket this message belongs to. */
    val ticketId: Long

}
